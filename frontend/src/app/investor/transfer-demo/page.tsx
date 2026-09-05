"use client";

import { useMemo, useState } from "react";
import { useQueries } from "@tanstack/react-query";
import { useAccount, usePublicClient, useWriteContract, useWaitForTransactionReceipt } from "wagmi";
import type { Address } from "viem";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { ConnectWalletButton } from "@/components/domain/ConnectWalletButton";
import { BlockchainTxDetails } from "@/components/domain/BlockchainTxDetails";
import { CheckCircle, XCircle } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken } from "@/lib/auth/session";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { tokenizationApi } from "@/lib/api/tokenization";
import { queryKeys } from "@/lib/api/query-keys";
import { trexTokenAbi, identityRegistryAbi } from "@/lib/wallet/trex-abi";
import { NEXT_PUBLIC_CHAIN_ID, NEXT_PUBLIC_CHAIN_LABEL } from "@/lib/utils/env";
import { walletAddressSchema } from "@/lib/validation/common";

type CheckState = "idle" | "checking" | "passed" | "failed";

export default function TransferDemoPage() {
  const token = useAccessToken();
  const { address: connectedAddress, isConnected } = useAccount();
  const publicClient = usePublicClient({ chainId: NEXT_PUBLIC_CHAIN_ID });
  const { writeContractAsync, data: txHash, isPending: isSubmitting } = useWriteContract();
  const receipt = useWaitForTransactionReceipt({ hash: txHash, chainId: NEXT_PUBLIC_CHAIN_ID });

  const investments = useAuthedQuery(queryKeys.investments.mine(), (t) => investmentsApi.mine(t));
  const settled = (investments.data ?? []).filter((i) => i.status === "SETTLED");
  // One option per distinct offering, not per investment — an investor can hold multiple
  // SETTLED investments in the same offering, which would otherwise render duplicate options.
  const heldOfferingIds = useMemo(() => Array.from(new Set(settled.map((i) => i.offeringId))), [settled]);
  const heldOfferingQueries = useQueries({
    queries: heldOfferingIds.map((id) => ({ queryKey: queryKeys.offerings.detail(id), queryFn: () => offeringsApi.get(id, token), enabled: !!token })),
  });
  const offeringOptions = heldOfferingIds.map((id, i) => ({ value: id, label: heldOfferingQueries[i]?.data?.name ?? id }));

  const [offeringId, setOfferingId] = useState<string>("");
  const [recipient, setRecipient] = useState("");
  const [units, setUnits] = useState("1");
  const [identityCheck, setIdentityCheck] = useState<CheckState>("idle");
  const [eligibilityCheck, setEligibilityCheck] = useState<CheckState>("idle");
  const [complianceCheck, setComplianceCheck] = useState<CheckState>("idle");
  const [rejectionMessage, setRejectionMessage] = useState<string | undefined>();
  const [tokenAddress, setTokenAddress] = useState<Address | undefined>();
  const [running, setRunning] = useState(false);

  function reset() {
    setIdentityCheck("idle");
    setEligibilityCheck("idle");
    setComplianceCheck("idle");
    setRejectionMessage(undefined);
  }

  async function handleAttemptTransfer() {
    reset();
    const parsedRecipient = walletAddressSchema.safeParse(recipient);
    if (!parsedRecipient.success || !publicClient || !connectedAddress || !offeringId) return;
    setRunning(true);

    try {
      const deployments = await tokenizationApi.deployments(offeringId, token);
      const tokenDeployment = deployments.find((d) => d.contractType === "TOKEN" && d.status === "CONFIRMED");
      if (!tokenDeployment?.contractAddress) {
        setRejectionMessage("This offering's token has not been deployed on-chain yet.");
        setRunning(false);
        return;
      }
      const tokenAddr = tokenDeployment.contractAddress as Address;
      setTokenAddress(tokenAddr);

      setIdentityCheck("checking");
      const registryAddress = await publicClient.readContract({
        address: tokenAddr,
        abi: trexTokenAbi,
        functionName: "identityRegistry",
      });
      const isVerified = await publicClient.readContract({
        address: registryAddress,
        abi: identityRegistryAbi,
        functionName: "isVerified",
        args: [recipient as Address],
      });

      if (!isVerified) {
        setIdentityCheck("failed");
        setEligibilityCheck("failed");
        setRejectionMessage("Recipient is not eligible to hold this investment instrument.");
        setRunning(false);
        return;
      }
      setIdentityCheck("passed");

      setEligibilityCheck("checking");
      // V1 simplification: on-chain eligibility reduces to identity verification (no claim topics configured yet) — see onchain/README.md#compliance.
      setEligibilityCheck("passed");

      setComplianceCheck("checking");
      const unitsBigInt = BigInt(units || "0");
      try {
        await publicClient.simulateContract({
          account: connectedAddress,
          address: tokenAddr,
          abi: trexTokenAbi,
          functionName: "transfer",
          args: [recipient as Address, unitsBigInt],
        });
      } catch (simulationError) {
        setComplianceCheck("failed");
        setRejectionMessage(extractRevertReason(simulationError) ?? "Recipient is not eligible to hold this investment instrument.");
        setRunning(false);
        return;
      }
      setComplianceCheck("passed");

      await writeContractAsync({
        account: connectedAddress,
        address: tokenAddr,
        abi: trexTokenAbi,
        functionName: "transfer",
        args: [recipient as Address, unitsBigInt],
        chainId: NEXT_PUBLIC_CHAIN_ID,
      });
    } catch (err) {
      setRejectionMessage(extractRevertReason(err) ?? "Transfer could not be submitted.");
    } finally {
      setRunning(false);
    }
  }

  const isRejected = identityCheck === "failed" || complianceCheck === "failed";
  const isConfirmed = receipt.data?.status === "success";

  return (
    <div className="max-w-2xl">
      <PageHeader
        title="Controlled Transfer Demo"
        description="Demonstrates ERC-3643's on-chain compliance enforcement: a transfer to an ineligible wallet is rejected by the token contract itself."
      />

      <Callout tone="info" className="mb-6">
        This performs a real on-chain transaction from your connected wallet. It requires your wallet to hold units of
        the selected offering and be connected to {NEXT_PUBLIC_CHAIN_LABEL}.
      </Callout>

      <Card>
        <CardHeader>
          <CardTitle>Attempt Transfer</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-5">
          {!isConnected && (
            <div className="flex items-center gap-3">
              <ConnectWalletButton />
              <span className="text-sm text-ink-500">Connect your wallet to run this demo.</span>
            </div>
          )}

          <Field label="Offering" htmlFor="offeringId" required>
            <Select
              id="offeringId"
              value={offeringId}
              onValueChange={(v) => {
                setOfferingId(v);
                reset();
              }}
              options={offeringOptions}
              placeholder="Select an offering you hold units in"
            />
          </Field>

          <Field label="Recipient Wallet Address" htmlFor="recipient" required hint="Try an unverified wallet, then a verified one.">
            <Input id="recipient" placeholder="0x…" value={recipient} onChange={(e) => setRecipient(e.target.value)} />
          </Field>

          <Field label="Units" htmlFor="units" required>
            <Input id="units" type="number" min="1" step="1" value={units} onChange={(e) => setUnits(e.target.value)} />
          </Field>

          <Button
            onClick={handleAttemptTransfer}
            isLoading={running || isSubmitting || receipt.isLoading}
            disabled={!isConnected || !offeringId || !recipient}
          >
            Attempt Transfer
          </Button>

          {(identityCheck !== "idle" || eligibilityCheck !== "idle" || complianceCheck !== "idle") && (
            <div className="flex flex-col gap-2 rounded-lg border border-surface-border bg-surface-subtle p-4">
              <CheckRow label="Checking recipient identity" state={identityCheck} />
              <CheckRow label="Checking eligibility" state={eligibilityCheck} />
              <CheckRow label="Checking compliance" state={complianceCheck} />
            </div>
          )}

          {isRejected && (
            <div className="flex flex-col items-center gap-2 rounded-lg border border-danger-500/30 bg-danger-50 py-8 text-center">
              <XCircle className="h-8 w-8 text-danger-600" />
              <p className="text-base font-semibold text-danger-700">TRANSFER REJECTED</p>
              <p className="max-w-sm text-sm text-danger-700/90">{rejectionMessage}</p>
            </div>
          )}

          {!isRejected && txHash && (
            <div className="flex flex-col gap-3">
              {isConfirmed && (
                <div className="flex flex-col items-center gap-2 rounded-lg border border-success-500/30 bg-success-50 py-6 text-center">
                  <CheckCircle className="h-8 w-8 text-success-600" />
                  <p className="text-base font-semibold text-success-700">TRANSFER CONFIRMED</p>
                </div>
              )}
              <BlockchainTxDetails
                status={receipt.isLoading ? "PENDING" : isConfirmed ? "CONFIRMED" : "FAILED"}
                network={NEXT_PUBLIC_CHAIN_LABEL.toLowerCase().includes("sepolia") ? "sepolia" : "anvil"}
                txHash={txHash}
                contractAddress={tokenAddress}
                blockNumber={receipt.data?.blockNumber ? Number(receipt.data.blockNumber) : null}
              />
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function CheckRow({ label, state }: { label: string; state: CheckState }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="text-ink-700">{label}</span>
      {state === "idle" && <span className="text-ink-400">—</span>}
      {state === "checking" && <span className="text-gold-600">Checking…</span>}
      {state === "passed" && (
        <span className="flex items-center gap-1 font-medium text-success-700">
          <CheckCircle className="h-4 w-4" /> PASSED
        </span>
      )}
      {state === "failed" && (
        <span className="flex items-center gap-1 font-medium text-danger-700">
          <XCircle className="h-4 w-4" /> FAILED
        </span>
      )}
    </div>
  );
}

function extractRevertReason(error: unknown): string | undefined {
  if (error && typeof error === "object" && "shortMessage" in error && typeof error.shortMessage === "string") {
    return error.shortMessage;
  }
  if (error instanceof Error) return error.message;
  return undefined;
}
