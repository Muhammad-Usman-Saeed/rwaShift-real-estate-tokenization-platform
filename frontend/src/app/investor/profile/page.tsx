"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useQueryClient } from "@tanstack/react-query";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { StatusPill } from "@/components/domain/StatusPill";
import { ConnectWalletButton } from "@/components/domain/ConnectWalletButton";
import { ApiError } from "@/lib/errors/problem-details";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useCurrentUser } from "@/lib/auth/session";
import { useLinkedWallet } from "@/lib/wallet/useLinkedWallet";
import { useToast } from "@/components/ui/Toast";
import { investorsApi } from "@/lib/api/investors";
import { kycApi } from "@/lib/api/kyc";
import { queryKeys } from "@/lib/api/query-keys";
import { countryCodeSchema, walletAddressSchema } from "@/lib/validation/common";
import type { InvestorType } from "@/lib/api/types";

const onboardSchema = z.object({
  investorType: z.enum(["INDIVIDUAL", "ORGANIZATION"]),
  displayName: z.string().min(1, "Name is required").max(300),
  countryCode: countryCodeSchema,
  dateOfBirth: z.string().optional(),
  primaryWalletAddress: walletAddressSchema,
});
type OnboardValues = z.infer<typeof onboardSchema>;

export default function InvestorProfilePage() {
  const { userId } = useCurrentUser();
  const investor = useAuthedQuery(queryKeys.investors.me(), (token) => investorsApi.me(token));

  const notOnboarded = investor.isError && investor.error instanceof ApiError && investor.error.status === 404;

  if (investor.isLoading) return null;

  if (notOnboarded) return <OnboardingForm userId={userId} />;
  if (investor.isError) return <ErrorBanner error={investor.error} />;
  if (!investor.data) return null;

  return <VerificationOverview investorId={investor.data.id} walletAddress={investor.data.primaryWalletAddress} investor={investor.data} />;
}

function OnboardingForm({ userId }: { userId?: string }) {
  const queryClient = useQueryClient();
  const { update } = useSession();
  const { push } = useToast();
  const { address, isConnected, connect, isConnecting } = useLinkedWallet();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<OnboardValues>({
    resolver: zodResolver(onboardSchema),
    defaultValues: { investorType: "INDIVIDUAL", countryCode: "AE" },
  });

  const onboard = useAuthedMutation((token, values: OnboardValues) =>
    investorsApi.onboard({ userId: userId as string, ...values }, token),
  );

  useEffect(() => {
    if (isConnected && address) {
      setValue("primaryWalletAddress", address, { shouldValidate: true });
    }
  }, [isConnected, address, setValue]);

  return (
    <div className="max-w-xl">
      <PageHeader title="Complete Your Profile" description="Set up your investor profile and link a wallet to begin investing." />
      <Card>
        <CardContent>
          {onboard.isError && <ErrorBanner error={onboard.error} className="mb-4" />}
          <form
            className="flex flex-col gap-5"
            onSubmit={handleSubmit((values) =>
              onboard.mutate(values, {
                onSuccess: async () => {
                  // The backend's `investor_id` JWT claim only reflects the new profile once a
                  // fresh token is issued — force that now instead of leaving the investor stuck
                  // behind the onboarding gate until their current access token naturally expires.
                  await update();
                  queryClient.invalidateQueries({ queryKey: queryKeys.investors.me() });
                  push({ title: "Profile created", variant: "success" });
                },
                onError: () => push({ title: "Could not create profile", variant: "error" }),
              }),
            )}
          >
            <Field label="Investor Type" htmlFor="investorType" required>
              <Controller
                control={control}
                name="investorType"
                render={({ field }) => (
                  <Select
                    id="investorType"
                    value={field.value}
                    onValueChange={field.onChange}
                    options={[
                      { value: "INDIVIDUAL", label: "Individual" },
                      { value: "ORGANIZATION", label: "Organization" },
                    ] satisfies { value: InvestorType; label: string }[]}
                  />
                )}
              />
            </Field>
            <Field label="Full Name" htmlFor="displayName" required error={errors.displayName?.message}>
              <Input id="displayName" {...register("displayName")} invalid={!!errors.displayName} />
            </Field>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Country" htmlFor="countryCode" required error={errors.countryCode?.message} hint="2-letter ISO code">
                <Input id="countryCode" maxLength={2} className="uppercase" {...register("countryCode")} invalid={!!errors.countryCode} />
              </Field>
              <Field label="Date of Birth" htmlFor="dateOfBirth" hint="Optional">
                <Input id="dateOfBirth" type="date" {...register("dateOfBirth")} />
              </Field>
            </div>
            <Field label="Wallet Address" htmlFor="primaryWalletAddress" required error={errors.primaryWalletAddress?.message}>
              <div className="flex gap-2">
                <Input
                  id="primaryWalletAddress"
                  placeholder="0x…"
                  {...register("primaryWalletAddress")}
                  invalid={!!errors.primaryWalletAddress}
                />
                {!isConnected && (
                  <Button
                    type="button"
                    variant="secondary"
                    isLoading={isConnecting}
                    onClick={() =>
                      connect()
                    }
                  >
                    Connect
                  </Button>
                )}
              </div>
            </Field>

            <div className="flex justify-end">
              <Button type="submit" isLoading={onboard.isPending}>
                Complete Profile
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function VerificationOverview({
  investorId,
  walletAddress,
  investor,
}: {
  investorId: string;
  walletAddress: string | null;
  investor: import("@/lib/api/types").InvestorResponse;
}) {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [linkedWalletInput, setLinkedWalletInput] = useState(walletAddress ?? "");

  const kyc = useAuthedQuery(queryKeys.kyc.forInvestor(investorId), (token) => kycApi.getByInvestor(investorId, token));
  const kycNotStarted = kyc.isError && kyc.error instanceof ApiError && kyc.error.status === 404;

  const submitKyc = useAuthedMutation((token) => kycApi.submit(investorId, token));
  const linkWallet = useAuthedMutation((token) => investorsApi.linkWallet(linkedWalletInput, token));
  const { address, isConnected, connect } = useLinkedWallet(walletAddress);

  const kycStatus = kycNotStarted ? "NOT_STARTED" : kyc.data?.status;

  return (
    <div className="max-w-2xl">
      <PageHeader title="Profile & Verification" description="Your identity, wallet, and compliance verification status." />

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Profile</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <Term label="Name" value={investor.displayName} />
              <Term label="Investor Type" value={investor.investorType} />
              <Term label="Country" value={investor.countryCode} />
              <Term label="Status" value={<StatusPill status={investor.status} />} />
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Wallet</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <p className="text-sm text-ink-600">
              Linked wallet: <span className="font-mono text-ink-800">{walletAddress ?? "None"}</span>
            </p>
            <div className="flex flex-wrap items-center gap-2">
              <ConnectWalletButton linkedAddress={walletAddress} />
              {isConnected && address && address.toLowerCase() !== (walletAddress ?? "").toLowerCase() && (
                <Button
                  size="sm"
                  isLoading={linkWallet.isPending}
                  onClick={() => {
                    setLinkedWalletInput(address);
                    linkWallet.mutate(undefined, {
                      onSuccess: () => {
                        queryClient.invalidateQueries({ queryKey: queryKeys.investors.me() });
                        push({ title: "Wallet linked", description: address, variant: "success" });
                      },
                      onError: () => push({ title: "Could not link wallet", variant: "error" }),
                    });
                  }}
                >
                  Link this wallet
                </Button>
              )}
            </div>
            {linkWallet.isError && <ErrorBanner error={linkWallet.error} />}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>KYC Verification</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <span className="text-sm text-ink-600">Status:</span>
              {kycStatus && <StatusPill status={kycStatus} />}
            </div>
            {kyc.data?.rejectionReason && <p className="text-sm text-danger-700">Reason: {kyc.data.rejectionReason}</p>}
            {(kycStatus === "NOT_STARTED" || kycStatus === "REJECTED") && (
              <div>
                {submitKyc.isError && <ErrorBanner error={submitKyc.error} className="mb-3" />}
                <Button
                  isLoading={submitKyc.isPending}
                  onClick={() =>
                    submitKyc.mutate(undefined, {
                      onSuccess: () => {
                        queryClient.invalidateQueries({ queryKey: queryKeys.kyc.forInvestor(investorId) });
                        push({ title: "KYC submitted", description: "Your verification is under review.", variant: "info" });
                      },
                    })
                  }
                >
                  Submit Verification
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Eligibility &amp; Blockchain Identity</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-ink-600">
              Eligibility and on-chain identity registration are evaluated automatically, per offering, once your KYC is
              verified — you&apos;ll see both statuses when you invest in a specific opportunity.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Term({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-500">{label}</dt>
      <dd className="mt-1 font-medium text-ink-900">{value}</dd>
    </div>
  );
}
