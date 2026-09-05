"use client";

import { useQueries } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Skeleton } from "@/components/ui/Skeleton";
import { OpportunityCard } from "@/components/domain/OpportunityCard";
import { Building } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken } from "@/lib/auth/session";
import { offeringsApi } from "@/lib/api/offerings";
import { assetsApi } from "@/lib/api/assets";
import { queryKeys } from "@/lib/api/query-keys";

export default function OpportunitiesPage() {
  const token = useAccessToken();
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));

  const assetQueries = useQueries({
    queries: (offerings.data ?? []).map((offering) => ({
      queryKey: queryKeys.assets.detail(offering.assetId),
      queryFn: () => assetsApi.get(offering.assetId, token),
      enabled: !!token,
    })),
  });

  return (
    <div>
      <PageHeader title="Investment Opportunities" description="Open real estate offerings available for investment." />

      {offerings.isError && <ErrorBanner error={offerings.error} />}

      {offerings.isLoading && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-72 w-full" />
          ))}
        </div>
      )}

      {offerings.data && offerings.data.length === 0 && (
        <EmptyState icon={<Building className="h-8 w-8" />} title="No open opportunities right now" description="Check back soon for new offerings." />
      )}

      {offerings.data && offerings.data.length > 0 && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {offerings.data.map((offering, index) => (
            <OpportunityCard key={offering.id} offering={offering} asset={assetQueries[index]?.data} />
          ))}
        </div>
      )}
    </div>
  );
}
