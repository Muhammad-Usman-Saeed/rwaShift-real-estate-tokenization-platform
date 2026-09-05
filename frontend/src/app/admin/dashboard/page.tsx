"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { StatTile } from "@/components/ui/StatTile";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Building, CircleDollar, Layers, ShieldCheck, Users } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { organizationsApi } from "@/lib/api/organizations";
import { assetsApi } from "@/lib/api/assets";
import { offeringsApi } from "@/lib/api/offerings";
import { investorsApi } from "@/lib/api/investors";
import { kycApi } from "@/lib/api/kyc";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCompactCurrency, formatNumber } from "@/lib/utils/format";

export default function AdminDashboardPage() {
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (t) => organizationsApi.list(t));
  const assets = useAuthedQuery(queryKeys.assets.list(), (t) => assetsApi.list(t));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));
  const investors = useAuthedQuery(queryKeys.investors.list(), (t) => investorsApi.list(t));
  const kycCases = useAuthedQuery(queryKeys.kyc.cases(), (t) => kycApi.listCases(t));

  const isLoading = organizations.isLoading || assets.isLoading || offerings.isLoading;
  const pendingApprovals = offerings.data?.filter((o) => o.status === "UNDER_REVIEW").length ?? 0;
  const pendingKyc = kycCases.data?.filter((c) => c.status === "SUBMITTED" || c.status === "UNDER_REVIEW").length ?? 0;
  const totalValuation = assets.data?.reduce((sum, a) => sum + a.valuation, 0) ?? 0;

  return (
    <div>
      <PageHeader title="Dashboard" description="Platform-wide activity across all organizations." />

      {isLoading && <SkeletonText lines={4} />}
      {organizations.isError && <ErrorBanner error={organizations.error} />}

      {!isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatTile label="Organizations" value={formatNumber(organizations.data?.length ?? 0)} icon={<Building className="h-5 w-5" />} />
          <StatTile label="Total Assets" value={formatNumber(assets.data?.length ?? 0)} icon={<Layers className="h-5 w-5" />} />
          <StatTile label="Tokenized Value" value={formatCompactCurrency(totalValuation)} icon={<CircleDollar className="h-5 w-5" />} />
          <StatTile label="Offerings" value={formatNumber(offerings.data?.length ?? 0)} icon={<Layers className="h-5 w-5" />} />
          <StatTile label="Pending Approvals" value={formatNumber(pendingApprovals)} icon={<ShieldCheck className="h-5 w-5" />} />
          <StatTile label="Registered Investors" value={formatNumber(investors.data?.length ?? 0)} icon={<Users className="h-5 w-5" />} />
          <StatTile label="Pending KYC Reviews" value={formatNumber(pendingKyc)} icon={<ShieldCheck className="h-5 w-5" />} />
        </div>
      )}
    </div>
  );
}
