"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { StatTile } from "@/components/ui/StatTile";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { reportingApi } from "@/lib/api/reporting";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCompactCurrency, formatNumber } from "@/lib/utils/format";
import { Building, CircleDollar, Coins, Layers, Receipt, Users } from "@/components/ui/icons";

export default function IssuerDashboardPage() {
  const summary = useAuthedQuery(queryKeys.reporting.issuerSummary(), (token) => reportingApi.issuerSummary(token));

  return (
    <div>
      <PageHeader title="Dashboard" description="A snapshot of your organization's tokenized real estate activity." />

      {summary.isLoading && <SkeletonText lines={4} />}
      {summary.isError && <ErrorBanner error={summary.error} />}

      {summary.data && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile label="Total Assets" value={formatNumber(summary.data.totalAssets)} icon={<Building className="h-5 w-5" />} />
            <StatTile
              label="Tokenized Asset Value"
              value={formatCompactCurrency(summary.data.totalAssetValuation)}
              icon={<Layers className="h-5 w-5" />}
            />
            <StatTile
              label="Capital Raised"
              value={formatCompactCurrency(summary.data.totalCapitalRaised)}
              icon={<CircleDollar className="h-5 w-5" />}
            />
            <StatTile label="Active Offerings" value={formatNumber(summary.data.activeOfferings)} icon={<Layers className="h-5 w-5" />} />
            <StatTile label="Investor Count" value={formatNumber(summary.data.distinctInvestorCount)} icon={<Users className="h-5 w-5" />} />
            <StatTile label="Units Issued" value={formatNumber(summary.data.totalUnitsIssued)} icon={<Coins className="h-5 w-5" />} />
            <StatTile label="Available Units" value={formatNumber(summary.data.totalUnitsAvailable)} icon={<Coins className="h-5 w-5" />} />
            <StatTile label="Distributions" value={formatNumber(summary.data.distributionCount)} icon={<Receipt className="h-5 w-5" />} />
          </div>

          <Card className="mt-6">
            <CardHeader>
              <CardTitle>Unit Issuance Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <UnitProgressBar issued={summary.data.totalUnitsIssued} available={summary.data.totalUnitsAvailable} />
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}

function UnitProgressBar({ issued, available }: { issued: number; available: number }) {
  const total = issued + available;
  const pct = total > 0 ? Math.round((issued / total) * 100) : 0;
  return (
    <div>
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-ink-800">{formatNumber(issued)} units issued</span>
        <span className="text-ink-500">{formatNumber(available)} available</span>
      </div>
      <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-surface-muted">
        <div className="h-full rounded-full bg-brand-600" style={{ width: `${pct}%` }} />
      </div>
      <p className="mt-1 text-xs text-ink-500">{pct}% of total capacity issued across all offerings</p>
    </div>
  );
}
