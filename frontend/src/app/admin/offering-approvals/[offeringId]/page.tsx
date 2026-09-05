"use client";

import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { StatusPill } from "@/components/domain/StatusPill";
import { DocumentsPanel } from "@/components/domain/DocumentsPanel";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { offeringsApi } from "@/lib/api/offerings";
import { assetsApi } from "@/lib/api/assets";
import { legalStructuresApi } from "@/lib/api/legal-structures";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatPercent } from "@/lib/utils/format";

export default function OfferingApprovalDetailPage({ params }: { params: { offeringId: string } }) {
  const { offeringId } = params;
  const queryClient = useQueryClient();
  const { push } = useToast();

  const offering = useAuthedQuery(queryKeys.offerings.detail(offeringId), (t) => offeringsApi.get(offeringId, t));
  const asset = useAuthedQuery(
    queryKeys.assets.detail(offering.data?.assetId ?? ""),
    (t) => assetsApi.get(offering.data!.assetId, t),
    { enabled: !!offering.data },
  );
  const legalStructure = useAuthedQuery(
    queryKeys.legalStructures.detail(offering.data?.legalStructureId ?? ""),
    (t) => legalStructuresApi.get(offering.data!.legalStructureId, t),
    { enabled: !!offering.data },
  );
  const organization = useAuthedQuery(
    queryKeys.organizations.detail(offering.data?.organizationId ?? ""),
    (t) => organizationsApi.get(offering.data!.organizationId, t),
    { enabled: !!offering.data },
  );

  const approve = useAuthedMutation((token) => offeringsApi.approve(offeringId, token));
  const reject = useAuthedMutation((token) => offeringsApi.reject(offeringId, token));

  if (offering.isLoading) return <SkeletonText lines={6} />;
  if (offering.isError || !offering.data) return <ErrorBanner error={offering.error} />;

  const data = offering.data;
  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.offerings.detail(offeringId) });

  return (
    <div className="max-w-3xl">
      <PageHeader
        title={data.name}
        breadcrumb={
          <Link href="/admin/offering-approvals" className="hover:underline">
            Offering Approvals
          </Link>
        }
        actions={<StatusPill status={data.status} />}
      />

      {(approve.isError || reject.isError) && <ErrorBanner error={approve.error ?? reject.error} className="mb-4" />}

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Submission Detail</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <Term label="Asset" value={asset.data?.name} />
            <Term label="Issuer" value={organization.data?.displayName} />
            <Term label="Legal Structure" value={legalStructure.data?.legalEntityName} />
            <Term label="Asset Valuation" value={asset.data ? formatCurrency(asset.data.valuation, asset.data.currency) : undefined} />
            <Term label="Raise" value={formatCurrency(data.targetRaise, data.currency)} />
            <Term label="Total Units" value={data.totalUnits.toLocaleString()} />
            <Term label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
            <Term label="Offered Interest" value={formatPercent(data.offeredInterestPercentage)} />
            <Term label="Minimum Investment" value={formatCurrency(data.minimumInvestment, data.currency)} />
          </dl>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Documents</CardTitle>
        </CardHeader>
        <CardContent>
          <DocumentsPanel resourceType="Offering" resourceId={offeringId} />
        </CardContent>
      </Card>

      {data.status === "UNDER_REVIEW" && (
        <div className="flex justify-end gap-3">
          <Button
            variant="danger"
            isLoading={reject.isPending}
            onClick={() =>
              reject.mutate(undefined, {
                onSuccess: () => {
                  invalidate();
                  push({ title: "Offering rejected", variant: "info" });
                },
              })
            }
          >
            Reject
          </Button>
          <Button
            isLoading={approve.isPending}
            onClick={() =>
              approve.mutate(undefined, {
                onSuccess: () => {
                  invalidate();
                  push({ title: "Offering approved", variant: "success" });
                },
              })
            }
          >
            Approve
          </Button>
        </div>
      )}

      {data.status === "APPROVED" && (
        <div className="flex justify-end">
          <Link href={`/issuer/offerings/${offeringId}/tokenize`}>
            <Button>Tokenize Offering</Button>
          </Link>
        </div>
      )}
    </div>
  );
}

function Term({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-500">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-ink-900">{value ?? "—"}</dd>
    </div>
  );
}
