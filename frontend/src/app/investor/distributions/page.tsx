"use client";

import { useQueries } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import { StatusPill } from "@/components/domain/StatusPill";
import { StatTile } from "@/components/ui/StatTile";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Coins } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken, useCurrentUser } from "@/lib/auth/session";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { distributionsApi } from "@/lib/api/distributions";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDate } from "@/lib/utils/format";

export default function InvestorDistributionsPage() {
  const token = useAccessToken();
  const { investorId } = useCurrentUser();
  const investments = useAuthedQuery(queryKeys.investments.mine(), (t) => investmentsApi.mine(t));

  const offeringIds = Array.from(new Set((investments.data ?? []).map((i) => i.offeringId)));
  const offeringQueries = useQueries({
    queries: offeringIds.map((id) => ({ queryKey: queryKeys.offerings.detail(id), queryFn: () => offeringsApi.get(id, token), enabled: !!token })),
  });
  const distributionListQueries = useQueries({
    queries: offeringIds.map((id) => ({
      queryKey: [...queryKeys.distributions.all, "offering", id],
      queryFn: () => distributionsApi.forOffering(id, token),
      enabled: !!token,
    })),
  });

  const allDistributions = distributionListQueries.flatMap((q, i) => (q.data ?? []).map((d) => ({ ...d, offeringName: offeringQueries[i]?.data?.name })));

  const entitlementQueries = useQueries({
    queries: allDistributions.map((d) => ({
      queryKey: queryKeys.distributions.entitlements(d.id),
      queryFn: () => distributionsApi.entitlements(d.id, token),
      enabled: !!token && d.status !== "DRAFT",
    })),
  });

  const rows = allDistributions.map((d, i) => {
    const mine = entitlementQueries[i]?.data?.find((e) => e.investorId === investorId);
    return { ...d, myEntitlement: mine?.entitlementAmount };
  });

  const totalDistributions = rows.reduce((sum, r) => sum + (r.myEntitlement ?? 0), 0);
  const isLoading = investments.isLoading || offeringQueries.some((q) => q.isLoading) || distributionListQueries.some((q) => q.isLoading);

  return (
    <div>
      <PageHeader title="Distributions" description="Rental income and other distributions paid to you." />

      <div className="mb-6">
        <StatTile label="Total Distributions" value={formatCurrency(totalDistributions)} icon={<Coins className="h-5 w-5" />} />
      </div>

      {isLoading && <SkeletonText lines={4} />}
      {investments.isError && <ErrorBanner error={investments.error} />}

      {!isLoading && rows.length === 0 && <EmptyState icon={<Coins className="h-8 w-8" />} title="No distributions yet" />}

      {!isLoading && rows.length > 0 && (
        <div className="flex flex-col gap-3">
          {rows
            .filter((r) => r.myEntitlement !== undefined)
            .map((d) => (
              <Card key={d.id}>
                <CardContent className="flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-ink-900">{d.offeringName}</p>
                    <p className="text-sm text-ink-500">Record date {formatDate(d.recordDate)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold tabular-nums text-ink-900">{formatCurrency(d.myEntitlement ?? 0, d.currency)}</p>
                    <div className="mt-1 flex items-center justify-end gap-2">
                      <StatusPill status={d.status === "COMPLETED" ? "COMPLETED" : d.status} />
                      <span className="text-xs text-ink-400">SIMULATED</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
        </div>
      )}
    </div>
  );
}
