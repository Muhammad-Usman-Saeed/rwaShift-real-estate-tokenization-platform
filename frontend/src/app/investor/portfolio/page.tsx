"use client";

import { useQueries } from "@tanstack/react-query";
import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatTile } from "@/components/ui/StatTile";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { InvestmentCard } from "@/components/domain/InvestmentCard";
import { CircleDollar, Coins, Layers } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken } from "@/lib/auth/session";
import { useCurrentUser } from "@/lib/auth/session";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { reportingApi } from "@/lib/api/reporting";
import { ownershipApi } from "@/lib/api/ownership";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, truncateHex } from "@/lib/utils/format";
import { OnChainHoldingRow } from "@/components/domain/OnChainHoldingRow";

export default function PortfolioPage() {
  const token = useAccessToken();
  const { investorId } = useCurrentUser();

  const investments = useAuthedQuery(queryKeys.investments.mine(), (t) => investmentsApi.mine(t));
  const summary = useAuthedQuery(
    queryKeys.reporting.investorSummary(investorId ?? ""),
    (t) => reportingApi.investorSummary(investorId as string, t),
    { enabled: !!investorId },
  );
  const ownership = useAuthedQuery(
    queryKeys.ownership.forInvestor(investorId ?? ""),
    (t) => ownershipApi.forInvestor(investorId as string, t),
    { enabled: !!investorId },
  );

  const offeringIds = Array.from(new Set((investments.data ?? []).map((i) => i.offeringId)));
  const offeringQueries = useQueries({
    queries: offeringIds.map((id) => ({
      queryKey: queryKeys.offerings.detail(id),
      queryFn: () => offeringsApi.get(id, token),
      enabled: !!token,
    })),
  });
  const offeringsById = Object.fromEntries(offeringIds.map((id, i) => [id, offeringQueries[i]?.data]));

  const activeCount = (investments.data ?? []).filter((i) => i.status === "SETTLED").length;

  return (
    <div>
      <PageHeader title="My Portfolio" description="Your tokenized real estate investments." />

      {investments.isError && <ErrorBanner error={investments.error} />}

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatTile label="Total Invested" value={summary.data ? formatCurrency(summary.data.totalInvested) : "—"} icon={<CircleDollar className="h-5 w-5" />} />
        <StatTile label="Active Investments" value={activeCount} icon={<Layers className="h-5 w-5" />} />
        <StatTile label="Total Units Owned" value={summary.data?.totalUnitsOwned ?? "—"} icon={<Coins className="h-5 w-5" />} />
      </div>

      {investments.isLoading && <SkeletonText lines={4} />}

      {investments.data && investments.data.length === 0 && (
        <EmptyState title="No investments yet" description="Browse investment opportunities to get started." />
      )}

      {investments.data && investments.data.length > 0 && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {investments.data.map((investment) => (
            <InvestmentCard key={investment.id} investment={investment} offering={offeringsById[investment.offeringId]} />
          ))}
        </div>
      )}

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>On-Chain Holdings</CardTitle>
          <Link href="/investor/transfer-demo">
            <Button variant="secondary" size="sm">
              Try a Compliance-Controlled Transfer
            </Button>
          </Link>
        </CardHeader>
        <CardContent>
          <p className="mb-4 text-xs text-ink-500">
            Live on-chain balances, distinct from the application investment records above — read directly from the
            ERC-3643 token contracts.
          </p>
          {ownership.isLoading && <SkeletonText lines={2} />}
          {ownership.data && ownership.data.length === 0 && <p className="text-sm text-ink-500">No on-chain holdings found yet.</p>}
          {ownership.data && ownership.data.length > 0 && (
            <div className="flex flex-col gap-3">
              {ownership.data.map((record) => (
                <OnChainHoldingRow key={`${record.tokenAddress}-${record.walletAddress}`} record={record} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
