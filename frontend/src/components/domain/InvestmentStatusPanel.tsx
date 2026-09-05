"use client";

import Link from "next/link";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  useAccount,
  useChainId,
  useSwitchChain,
  useWriteContract,
  usePublicClient,
} from "wagmi";
import { parseUnits } from "viem";
import type { Address } from "viem";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { Callout } from "@/components/ui/Callout";
import { StatusPill } from "@/components/domain/StatusPill";
import { ConnectWalletButton } from "@/components/domain/ConnectWalletButton";
import { ProgressSteps, type Step } from "@/components/ui/ProgressSteps";
import { CheckCircle, XCircle } from "@/components/ui/icons";
import { CopyButton } from "@/components/ui/CopyButton";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { investmentsApi } from "@/lib/api/investments";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, truncateHex } from "@/lib/utils/format";
import { erc20Abi, paymentRouterAbi } from "@/lib/wallet/trex-abi";
import { NEXT_PUBLIC_CHAIN_ID, NEXT_PUBLIC_CHAIN_LABEL } from "@/lib/utils/env";

const TERMINAL_STATUSES = ["SETTLED", "ELIGIBILITY_REJECTED", "PAYMENT_FAILED", "TOKEN_ISSUANCE_FAILED", "CANCELLED"];
const CANCELLABLE_STATUSES = ["ELIGIBILITY_PENDING", "PAYMENT_PENDING"];

/**
 * The single source of truth for "what's happening with this investment right now" — used both
 * right after creating one (invest wizard) and when returning to a still-pending one later (the
 * investments list' "Resume Payment" action, since a pending investment otherwise has no way back
 * to its deposit instructions once the wizard's local state is gone). Polls until a terminal
 * state is reached; a `CANCELLED`/failed/rejected investment stops polling and shows why.
 */
export function InvestmentStatusPanel({ investmentId, onViewPortfolio }: { investmentId: string; onViewPortfolio?: () => void }) {
  const queryClient = useQueryClient();
  const { push } = useToast();

  const investment = useAuthedQuery(queryKeys.investments.detail(investmentId), (token) => investmentsApi.get(investmentId, token), {
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && !TERMINAL_STATUSES.includes(status) ? 3000 : false;
    },
  });

  const cancel = useAuthedMutation((token) => investmentsApi.cancel(investmentId, token));

  function handleCancel() {
    cancel.mutate(undefined, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: queryKeys.investments.detail(investmentId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.investments.mine() });
        push({ title: "Investment cancelled", variant: "success" });
      },
      onError: () => push({ title: "Could not cancel investment", variant: "error" }),
    });
  }

  if (investment.isLoading || !investment.data) {
    if (investment.isError) {
      return (
        <Card>
          <CardContent>
            <ErrorBanner error={investment.error} />
          </CardContent>
        </Card>
      );
    }
    return (
      <Card>
        <CardContent>
          <SkeletonText lines={4} />
        </CardContent>
      </Card>
    );
  }

  const data = investment.data;

  if (data.status === "CANCELLED") {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
          <XCircle className="h-8 w-8 text-ink-400" />
          <p className="text-base font-semibold text-ink-700">Investment Cancelled</p>
          <p className="max-w-sm text-sm text-ink-500">You withdrew this investment before payment was confirmed.</p>
        </CardContent>
      </Card>
    );
  }

  if (data.status === "ELIGIBILITY_REJECTED") {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
          <XCircle className="h-8 w-8 text-danger-500" />
          <p className="text-base font-semibold text-danger-700">Not Eligible</p>
          <p className="max-w-sm text-sm text-ink-600">
            {data.failureReason ?? "You do not currently meet the eligibility requirements for this offering."}
          </p>
          <Link href="/investor/profile">
            <Button variant="secondary">Review Verification Status</Button>
          </Link>
        </CardContent>
      </Card>
    );
  }

  if (data.status === "PAYMENT_FAILED" || data.status === "TOKEN_ISSUANCE_FAILED") {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-10 text-center">
          <XCircle className="h-8 w-8 text-danger-500" />
          <p className="text-base font-semibold text-danger-700">Investment Could Not Be Completed</p>
          <p className="max-w-sm text-sm text-ink-600">{data.failureReason ?? "Please contact support."}</p>
        </CardContent>
      </Card>
    );
  }

  if (data.status === "SETTLED") {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-4 py-10 text-center">
          <CheckCircle className="h-10 w-10 text-success-600" />
          <p className="text-lg font-semibold text-ink-900">Investment Complete</p>
          <dl className="grid grid-cols-2 gap-6 text-sm">
            <SummaryItem label="Investment" value={formatCurrency(data.amount, data.currency)} />
            <SummaryItem label="Units" value={data.units.toLocaleString()} />
          </dl>
          <div className="flex items-center gap-2">
            <StatusPill status="CONFIRMED" />
            <span className="text-sm text-ink-600">Blockchain confirmed</span>
          </div>
          {onViewPortfolio && <Button onClick={onViewPortfolio}>View Portfolio</Button>}
        </CardContent>
      </Card>
    );
  }

  const isCrypto = data.paymentMethod === "CRYPTO_WALLET";
  const canCancel = CANCELLABLE_STATUSES.includes(data.status);
  const steps: Step[] = [
    { key: "eligibility", label: "Eligibility", state: data.status === "ELIGIBILITY_PENDING" ? "current" : "complete" },
    {
      key: "payment",
      label: isCrypto ? "Wallet Payment" : "Demo Payment",
      state: data.paymentStatus === "CONFIRMED" ? "complete" : data.status === "PAYMENT_PENDING" ? "current" : "upcoming",
    },
    {
      key: "issuance",
      label: "Token Issuance",
      state: data.tokenIssuanceStatus === "CONFIRMED" ? "complete" : data.paymentStatus === "CONFIRMED" ? "current" : "upcoming",
    },
    { key: "blockchain", label: "Blockchain", state: "upcoming" },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Processing Your Investment</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <ProgressSteps steps={steps} />

        {data.status === "PAYMENT_PENDING" &&
          (isCrypto ? (
            <CryptoPaymentPanel investmentId={data.id} />
          ) : (
            <Callout tone="warning">
              DEMO SETTLEMENT — payment confirmation in V1 is a simulated step performed by a platform operator, not a
              real bank transfer.
            </Callout>
          ))}

        <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
          <SummaryItem label="Investment Status" value={data.status.replaceAll("_", " ")} />
          <SummaryItem label="Payment" value={data.paymentStatus} />
          <SummaryItem label="Wallet" value="Linked" />
          <SummaryItem label="Token Issuance" value={data.tokenIssuanceStatus.replaceAll("_", " ")} />
        </dl>

        {cancel.isError && <ErrorBanner error={cancel.error} />}

        <div className="flex items-center justify-between">
          <p className="text-xs text-ink-500">This page updates automatically as your investment progresses.</p>
          {canCancel && (
            <Button variant="secondary" size="sm" onClick={handleCancel} isLoading={cancel.isPending}>
              Cancel Investment
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase text-ink-500">{label}</dt>
      <dd className="mt-0.5 font-semibold tabular-nums text-ink-900">{value}</dd>
    </div>
  );
}

type PayPhase = "idle" | "approving" | "paying" | "submitted" | "reverted";

/**
 * Pays through `RwaShiftPaymentRouter` rather than a plain USDC `transfer`, in two on-chain
 * steps: `approve` the router to spend the exact amount, then call the router's
 * `payInvestment(investmentId, amount)`, which tags the payment with this investment's id in the
 * `PaymentReceived` event — see onchain/src/core/RwaShiftPaymentRouter.sol and
 * docs/critical-analysis.md #3 for why a plain transfer wasn't enough (nothing tied it to a
 * specific investment, so two same-amount pending investments were ambiguous). Confirmation is
 * still automatic from here: `CryptoPaymentWatcher` (backend) reads the event and settles the
 * investment; this panel only submits the two transactions, it doesn't poll or confirm anything
 * itself — the parent panel's existing investment-status poll picks up the result.
 */
function CryptoPaymentPanel({ investmentId }: { investmentId: string }) {
  const { address: connectedAddress, isConnected } = useAccount();
  const chainId = useChainId();
  const { switchChainAsync, isPending: isSwitching } = useSwitchChain();
  const { writeContractAsync } = useWriteContract();
  const publicClient = usePublicClient({ chainId: NEXT_PUBLIC_CHAIN_ID });

  const [phase, setPhase] = useState<PayPhase>("idle");
  const [payError, setPayError] = useState<Error | null>(null);

  const wrongNetwork = isConnected && chainId !== NEXT_PUBLIC_CHAIN_ID;

  const instructions = useAuthedQuery(["investments", investmentId, "crypto-instructions"], (t) =>
    investmentsApi.cryptoPaymentInstructions(investmentId, t),
  );

  if (instructions.isLoading) return <SkeletonText lines={3} />;
  if (instructions.isError || !instructions.data) return <ErrorBanner error={instructions.error} />;

  const { paymentRouterAddress, usdcTokenAddress, amountUsdc } = instructions.data;

  async function handlePay() {
    if (!connectedAddress || !publicClient) return;
    setPayError(null);
    try {
      if (chainId !== NEXT_PUBLIC_CHAIN_ID) {
        await switchChainAsync({ chainId: NEXT_PUBLIC_CHAIN_ID });
      }
      const amount = parseUnits(amountUsdc.toString(), 6);

      setPhase("approving");
      const approveHash = await writeContractAsync({
        account: connectedAddress,
        address: usdcTokenAddress as Address,
        abi: erc20Abi,
        functionName: "approve",
        args: [paymentRouterAddress as Address, amount],
        chainId: NEXT_PUBLIC_CHAIN_ID,
      });
      const approveReceipt = await publicClient.waitForTransactionReceipt({ hash: approveHash });
      if (approveReceipt.status !== "success") {
        throw new Error("USDC approval transaction reverted");
      }

      setPhase("paying");
      const payHash = await writeContractAsync({
        account: connectedAddress,
        address: paymentRouterAddress as Address,
        abi: paymentRouterAbi,
        functionName: "payInvestment",
        args: [investmentId, amount],
        chainId: NEXT_PUBLIC_CHAIN_ID,
      });
      const payReceipt = await publicClient.waitForTransactionReceipt({ hash: payHash });
      setPhase(payReceipt.status === "success" ? "submitted" : "reverted");
    } catch (cause) {
      setPayError(cause instanceof Error ? cause : new Error("Payment failed"));
      setPhase("idle");
    }
  }

  const isBusy = phase === "approving" || phase === "paying" || isSwitching;

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-surface-border bg-surface-subtle p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-ink-500">Amount Due</p>
          <p className="text-lg font-semibold tabular-nums text-ink-900">{amountUsdc.toLocaleString()} USDC</p>
        </div>
        {!isConnected && <ConnectWalletButton />}
      </div>

      <p className="text-xs text-ink-500">
        Paid through the platform&apos;s payment contract at{" "}
        <span className="inline-flex items-center gap-1 font-mono">
          {truncateHex(paymentRouterAddress)}
          <CopyButton value={paymentRouterAddress} label="Copy payment contract address" className="h-4 w-4" />
        </span>{" "}
        on your connected network — this tags the payment with this exact investment, so it confirms automatically
        with no ambiguity, no further action needed once submitted.
      </p>

      {wrongNetwork && (
        <Callout tone="warning">
          Your wallet is on the wrong network. Switch to {NEXT_PUBLIC_CHAIN_LABEL} to pay.
        </Callout>
      )}

      {payError && !wrongNetwork && <ErrorBanner error={payError} />}

      {phase === "idle" &&
        (wrongNetwork ? (
          <Button onClick={() => switchChainAsync({ chainId: NEXT_PUBLIC_CHAIN_ID })} isLoading={isSwitching}>
            Switch to {NEXT_PUBLIC_CHAIN_LABEL}
          </Button>
        ) : (
          <Button onClick={handlePay} isLoading={isBusy} disabled={!isConnected}>
            Pay {amountUsdc.toLocaleString()} USDC with Wallet
          </Button>
        ))}

      {phase === "approving" && <span className="text-sm text-gold-600">Approving USDC spend…</span>}
      {phase === "paying" && <span className="text-sm text-gold-600">Submitting payment…</span>}
      {phase === "submitted" && (
        <span className="flex items-center gap-1 text-sm font-medium text-success-700">
          <CheckCircle className="h-4 w-4" /> Sent — waiting for the platform to detect it
        </span>
      )}
      {phase === "reverted" && (
        <span className="flex items-center gap-1 text-sm font-medium text-danger-700">
          <XCircle className="h-4 w-4" /> Payment transaction reverted — check your USDC balance and try again
        </span>
      )}
    </div>
  );
}
