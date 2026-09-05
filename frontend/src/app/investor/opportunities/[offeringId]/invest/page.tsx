"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { Callout } from "@/components/ui/Callout";
import { InvestmentStatusPanel } from "@/components/domain/InvestmentStatusPanel";
import { Coins, Building } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { offeringsApi } from "@/lib/api/offerings";
import { investmentsApi, type PaymentMethod } from "@/lib/api/investments";
import { investorsApi } from "@/lib/api/investors";
import { kycApi } from "@/lib/api/kyc";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";
import { ApiError } from "@/lib/errors/problem-details";

type FlowStep = "amount" | "review" | "processing";

export default function InvestPage({ params }: { params: { offeringId: string } }) {
  const { offeringId } = params;
  const router = useRouter();

  const offering = useAuthedQuery(queryKeys.offerings.detail(offeringId), (token) => offeringsApi.get(offeringId, token));

  const investor = useAuthedQuery(queryKeys.investors.me(), (token) => investorsApi.me(token));
  const notOnboarded = investor.isError && investor.error instanceof ApiError && investor.error.status === 404;

  const kyc = useAuthedQuery(
    queryKeys.kyc.forInvestor(investor.data?.id ?? ""),
    (token) => kycApi.getByInvestor(investor.data!.id, token),
    { enabled: !!investor.data },
  );
  const kycNotStarted = kyc.isError && kyc.error instanceof ApiError && kyc.error.status === 404;
  const kycVerified = kyc.data?.status === "VERIFIED";

  const [step, setStep] = useState<FlowStep>("amount");
  const [amountInput, setAmountInput] = useState("10000");
  const [amountError, setAmountError] = useState<string | undefined>();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CRYPTO_WALLET");
  const [agreedDocs, setAgreedDocs] = useState(false);
  const [agreedTerms, setAgreedTerms] = useState(false);
  const [investmentId, setInvestmentId] = useState<string | undefined>();
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  const initiate = useAuthedMutation((token) => {
    const amount = Number(amountInput);
    return investmentsApi.initiate({ offeringId, amount, paymentMethod }, idempotencyKey, token);
  });

  if (offering.isLoading || investor.isLoading) return <SkeletonText lines={6} />;
  if (offering.isError || !offering.data) return <ErrorBanner error={offering.error} />;

  if (notOnboarded) {
    return (
      <OnboardingRequiredCallout
        offeringId={offeringId}
        offeringName={offering.data.name}
        title="Complete your investor profile to invest"
        description="You need an investor profile — with your details and a linked wallet — before you can invest in this offering."
      />
    );
  }
  if (investor.isError) return <ErrorBanner error={investor.error} />;

  if (investor.data && kyc.isPending) {
    return <SkeletonText lines={6} />;
  }
  if (investor.data && kyc.isError && !kycNotStarted) return <ErrorBanner error={kyc.error} />;
  if (investor.data && !kycVerified) {
    return (
      <OnboardingRequiredCallout
        offeringId={offeringId}
        offeringName={offering.data.name}
        title="KYC verification required to invest"
        description={
          kycNotStarted
            ? "You haven't started identity verification yet. Submit your KYC verification before you can invest."
            : "Your identity verification is still in progress or was not approved. Check your verification status before you can invest."
        }
      />
    );
  }

  const data = offering.data;
  const amount = Number(amountInput) || 0;
  const units = data.unitPrice > 0 ? Math.floor(amount / data.unitPrice) : 0;
  const isExactMultiple = data.unitPrice > 0 && amount > 0 && amount % data.unitPrice === 0;

  function validateAmount(): boolean {
    if (!amount || amount <= 0) {
      setAmountError("Enter an amount");
      return false;
    }
    if (amount < data.minimumInvestment) {
      setAmountError(`Minimum investment is ${formatCurrency(data.minimumInvestment, data.currency)}`);
      return false;
    }
    if (!isExactMultiple) {
      setAmountError(`Amount must be an exact multiple of the unit price (${formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })})`);
      return false;
    }
    if (units > data.unitsAvailable) {
      setAmountError("Requested units exceed units available in this offering");
      return false;
    }
    setAmountError(undefined);
    return true;
  }

  function handleConfirmInvestment() {
    initiate.mutate(undefined, {
      onSuccess: (created) => {
        setInvestmentId(created.id);
        setStep("processing");
      },
    });
  }

  return (
    <div className="max-w-2xl">
      <PageHeader
        title={`Invest — ${data.name}`}
        breadcrumb={
          <Link href={`/investor/opportunities/${offeringId}`} className="hover:underline">
            {data.name}
          </Link>
        }
      />

      {step === "amount" && (
        <Card>
          <CardHeader>
            <CardTitle>Enter Amount</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <Field label="Investment Amount" htmlFor="amount" required error={amountError} hint={`Minimum ${formatCurrency(data.minimumInvestment, data.currency)} · Unit price ${formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })}`}>
              <Input id="amount" type="number" step={data.unitPrice} value={amountInput} onChange={(e) => setAmountInput(e.target.value)} invalid={!!amountError} />
            </Field>

            <div className="rounded-lg border border-surface-border bg-surface-subtle p-4">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-500">Investment Summary</p>
              <dl className="grid grid-cols-3 gap-3 text-sm">
                <SummaryItem label="Investment" value={formatCurrency(amount, data.currency)} />
                <SummaryItem label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
                <SummaryItem label="Units" value={units.toLocaleString()} />
              </dl>
            </div>

            <div className="flex justify-end">
              <Button
                onClick={() => {
                  if (validateAmount()) setStep("review");
                }}
              >
                Continue
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === "review" && (
        <Card>
          <CardHeader>
            <CardTitle>Review &amp; Confirm</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <dl className="grid grid-cols-3 gap-3 rounded-lg border border-surface-border bg-surface-subtle p-4 text-sm">
              <SummaryItem label="Investment" value={formatCurrency(amount, data.currency)} />
              <SummaryItem label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
              <SummaryItem label="Units" value={units.toLocaleString()} />
            </dl>

            <Callout tone="info">
              Eligibility is verified automatically when you confirm — you must have completed KYC and hold a linked,
              registered wallet. See Profile &amp; Verification for your current status.
            </Callout>

            <div>
              <p className="mb-2 text-sm font-medium text-ink-800">How would you like to pay?</p>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <PaymentMethodOption
                  icon={<Coins className="h-5 w-5" />}
                  title="Pay with Wallet"
                  description="Send USDC directly from your connected wallet. Confirms automatically on-chain."
                  selected={paymentMethod === "CRYPTO_WALLET"}
                  onSelect={() => setPaymentMethod("CRYPTO_WALLET")}
                />
                <PaymentMethodOption
                  icon={<Building className="h-5 w-5" />}
                  title="Bank Transfer"
                  description="Wire the funds; a platform operator confirms receipt (demo simulation)."
                  selected={paymentMethod === "BANK_TRANSFER"}
                  onSelect={() => setPaymentMethod("BANK_TRANSFER")}
                />
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <label className="flex items-start gap-2 text-sm text-ink-700">
                <input type="checkbox" className="mt-0.5" checked={agreedDocs} onChange={(e) => setAgreedDocs(e.target.checked)} />
                I reviewed the offering documents
              </label>
              <label className="flex items-start gap-2 text-sm text-ink-700">
                <input type="checkbox" className="mt-0.5" checked={agreedTerms} onChange={(e) => setAgreedTerms(e.target.checked)} />
                I agree to the investment terms
              </label>
            </div>

            {initiate.isError && <ErrorBanner error={initiate.error} />}

            <div className="flex justify-between">
              <Button variant="secondary" onClick={() => setStep("amount")}>
                Back
              </Button>
              <Button onClick={handleConfirmInvestment} isLoading={initiate.isPending} disabled={!agreedDocs || !agreedTerms}>
                Confirm Investment
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === "processing" && investmentId && (
        <InvestmentStatusPanel investmentId={investmentId} onViewPortfolio={() => router.push("/investor/portfolio")} />
      )}
    </div>
  );
}

function OnboardingRequiredCallout({
  offeringId,
  offeringName,
  title,
  description,
}: {
  offeringId: string;
  offeringName: string;
  title: string;
  description: string;
}) {
  return (
    <div className="max-w-2xl">
      <PageHeader
        title={`Invest — ${offeringName}`}
        breadcrumb={
          <Link href={`/investor/opportunities/${offeringId}`} className="hover:underline">
            {offeringName}
          </Link>
        }
      />
      <Card>
        <CardContent className="flex flex-col gap-4 py-8">
          <Callout tone="warning">
            <p className="font-semibold">{title}</p>
            <p className="mt-1 text-sm">{description}</p>
          </Callout>
          <div>
            <Link href="/investor/profile">
              <Button>Go to Profile &amp; Verification</Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
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

function PaymentMethodOption({
  icon,
  title,
  description,
  selected,
  onSelect,
}: {
  icon: ReactNode;
  title: string;
  description: string;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "flex flex-col items-start gap-2 rounded-lg border p-4 text-left transition-colors",
        selected ? "border-brand-600 bg-brand-50/60 ring-1 ring-brand-600" : "border-surface-border hover:bg-surface-subtle",
      )}
    >
      <div className={cn("rounded-md p-1.5", selected ? "text-brand-700" : "text-ink-500")}>{icon}</div>
      <p className="text-sm font-semibold text-ink-900">{title}</p>
      <p className="text-xs text-ink-500">{description}</p>
    </button>
  );
}
