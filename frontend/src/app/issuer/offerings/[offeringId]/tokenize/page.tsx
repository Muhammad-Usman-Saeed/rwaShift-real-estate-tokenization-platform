"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { StatusPill } from "@/components/domain/StatusPill";
import { BlockchainTxDetails } from "@/components/domain/BlockchainTxDetails";
import { CheckCircle } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { offeringsApi } from "@/lib/api/offerings";
import { tokenizationApi } from "@/lib/api/tokenization";
import { queryKeys } from "@/lib/api/query-keys";
import { NEXT_PUBLIC_CHAIN_LABEL } from "@/lib/utils/env";

const schema = z.object({
  symbol: z
    .string()
    .min(2, "Symbol must be 2–10 characters")
    .max(10, "Symbol must be 2–10 characters")
    .regex(/^[A-Z0-9]+$/, "Uppercase letters and numbers only"),
});
type FormValues = z.infer<typeof schema>;

const NARRATION = [
  "Preparing configuration",
  "Deploying identity infrastructure",
  "Configuring compliance",
  "Deploying ERC-3643 token",
  "Assigning agents",
  "Waiting for confirmation",
];

export default function TokenizeOfferingPage({ params }: { params: { offeringId: string } }) {
  const { offeringId } = params;
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [narrationIndex, setNarrationIndex] = useState(0);

  const offering = useAuthedQuery(queryKeys.offerings.detail(offeringId), (token) => offeringsApi.get(offeringId, token));
  const deployments = useAuthedQuery(
    queryKeys.tokenization.deployments(offeringId),
    (token) => tokenizationApi.deployments(offeringId, token),
    { refetchInterval: (query) => (query.state.data?.some((d) => d.status === "PENDING") ? 3000 : false) },
  );

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { symbol: "" } });

  const deploy = useAuthedMutation((token, values: FormValues) => tokenizationApi.deploy(offeringId, values.symbol, token));

  const tokenDeployment = deployments.data?.find((d) => d.contractType === "TOKEN");
  const isPending = tokenDeployment?.status === "PENDING";
  const isConfirmed = tokenDeployment?.status === "CONFIRMED";
  const isFailed = tokenDeployment?.status === "FAILED";
  const isOpen = offering.data?.status === "OPEN" || offering.data?.status === "FUNDED" || offering.data?.status === "CLOSED";

  useEffect(() => {
    if (!isPending) return;
    const interval = setInterval(() => setNarrationIndex((i) => Math.min(i + 1, NARRATION.length - 1)), 2500);
    return () => clearInterval(interval);
  }, [isPending]);

  useEffect(() => {
    // Re-poll offering status alongside deployments so we notice the two-phase (createOffering -> unpause) completion.
    if (!isPending && !isConfirmed) return;
    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: queryKeys.offerings.detail(offeringId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.tokenization.deployments(offeringId) });
    }, 4000);
    return () => clearInterval(interval);
  }, [isPending, isConfirmed, offeringId, queryClient]);

  if (offering.isLoading) return <SkeletonText lines={6} />;
  if (offering.isError || !offering.data) return <ErrorBanner error={offering.error} />;

  const data = offering.data;

  return (
    <div className="max-w-2xl">
      <PageHeader
        title="Tokenize Offering"
        breadcrumb={
          <Link href={`/issuer/offerings/${offeringId}`} className="hover:underline">
            {data.name}
          </Link>
        }
        description="Deploy this offering's ERC-3643 investment units on-chain."
      />

      <Card>
        <CardHeader>
          <CardTitle>Token Configuration</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-5">
          <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3">
            <Term label="Offering" value={data.name} />
            <Term label="Token Standard" value="ERC-3643" />
            <Term label="Token Name" value={data.name} />
            <Term label="Units" value={data.totalUnits.toLocaleString()} />
            <Term label="Network" value={NEXT_PUBLIC_CHAIN_LABEL} />
            <Term label="Status" value={<StatusPill status={data.status} />} />
          </dl>

          {!tokenDeployment && data.status === "APPROVED" && (
            <form
              className="flex items-end gap-3"
              onSubmit={handleSubmit((values) =>
                deploy.mutate(values, {
                  onSuccess: () => {
                    push({ title: "Deployment submitted", description: "Deploying token infrastructure on-chain.", variant: "info" });
                    queryClient.invalidateQueries({ queryKey: queryKeys.tokenization.deployments(offeringId) });
                    queryClient.invalidateQueries({ queryKey: queryKeys.offerings.detail(offeringId) });
                  },
                  onError: () => push({ title: "Deployment failed to submit", variant: "error" }),
                }),
              )}
            >
              <Field label="Token Symbol" htmlFor="symbol" required error={errors.symbol?.message} className="flex-1">
                <Input id="symbol" placeholder="RWDBT" {...register("symbol")} invalid={!!errors.symbol} className="uppercase" />
              </Field>
              <Button type="submit" isLoading={deploy.isPending}>
                Deploy Token Infrastructure
              </Button>
            </form>
          )}
          {deploy.isError && <ErrorBanner error={deploy.error} />}

          {(tokenDeployment || data.status === "TOKENIZING") && (
            <div className="rounded-lg border border-surface-border bg-surface-subtle p-4">
              {isPending && (
                <div className="mb-4 flex items-center gap-3">
                  <span className="relative flex h-2.5 w-2.5">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-gold-400 opacity-75" />
                    <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-gold-500" />
                  </span>
                  <p className="text-sm font-medium text-ink-800">{NARRATION[narrationIndex]}…</p>
                </div>
              )}
              {isConfirmed && !isOpen && (
                <div className="mb-4 flex items-center gap-2 text-sm font-medium text-brand-700">
                  <CheckCircle className="h-4 w-4" />
                  Token deployed — finalizing (unpausing for investment)…
                </div>
              )}
              {isOpen && (
                <div className="mb-4 flex items-center gap-2 text-sm font-semibold text-success-700">
                  <CheckCircle className="h-4 w-4" />
                  ACTIVE — offering is open for investment
                </div>
              )}
              {tokenDeployment && (
                <BlockchainTxDetails
                  status={tokenDeployment.status === "PENDING" ? "PENDING" : tokenDeployment.status === "CONFIRMED" ? "CONFIRMED" : "FAILED"}
                  network={tokenDeployment.network}
                  txHash={tokenDeployment.deploymentTxHash}
                  contractAddress={tokenDeployment.contractAddress}
                  blockNumber={tokenDeployment.deploymentBlock}
                />
              )}
              {isFailed && <p className="mt-3 text-sm text-danger-700">Deployment failed. Contact platform support.</p>}
            </div>
          )}

          {isOpen && (
            <Link href={`/issuer/offerings/${offeringId}`}>
              <Button variant="secondary">Back to Offering</Button>
            </Link>
          )}
        </CardContent>
      </Card>
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
