"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { OwnershipFlowDiagram } from "@/components/domain/OwnershipFlowDiagram";
import { DocumentsPanel } from "@/components/domain/DocumentsPanel";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { offeringsApi } from "@/lib/api/offerings";
import { assetsApi } from "@/lib/api/assets";
import { legalStructuresApi } from "@/lib/api/legal-structures";
import { tokenizationApi } from "@/lib/api/tokenization";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDate, formatPercent } from "@/lib/utils/format";

export default function OpportunityDetailPage({ params }: { params: { offeringId: string } }) {
  const { offeringId } = params;

  const offering = useAuthedQuery(queryKeys.offerings.detail(offeringId), (token) => offeringsApi.get(offeringId, token));
  const asset = useAuthedQuery(
    queryKeys.assets.detail(offering.data?.assetId ?? ""),
    (token) => assetsApi.get(offering.data!.assetId, token),
    { enabled: !!offering.data },
  );
  const legalStructure = useAuthedQuery(
    queryKeys.legalStructures.detail(offering.data?.legalStructureId ?? ""),
    (token) => legalStructuresApi.get(offering.data!.legalStructureId, token),
    { enabled: !!offering.data },
  );
  const deployments = useAuthedQuery(
    queryKeys.tokenization.deployments(offeringId),
    (token) => tokenizationApi.deployments(offeringId, token),
  );
  const tokenDeployment = deployments.data?.find((d) => d.contractType === "TOKEN" && d.status === "CONFIRMED");

  if (offering.isLoading) return <SkeletonText lines={6} />;
  if (offering.isError || !offering.data) return <ErrorBanner error={offering.error} />;

  const data = offering.data;
  const pctRaised = data.totalUnits > 0 ? (data.unitsIssued / data.totalUnits) * 100 : 0;
  const isInvestable = data.status === "OPEN";

  return (
    <div>
      <PageHeader
        title={asset.data?.name ?? data.name}
        breadcrumb={
          <Link href="/investor/opportunities" className="hover:underline">
            Opportunities
          </Link>
        }
        description={asset.data?.location}
        actions={
          isInvestable ? (
            <Link href={`/investor/opportunities/${offeringId}/invest`}>
              <Button>Invest in this Offering</Button>
            </Link>
          ) : undefined
        }
      />

      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatBlock label="Asset Value" value={asset.data ? formatCurrency(asset.data.valuation, asset.data.currency) : "—"} />
        <StatBlock label="Offering" value={formatCurrency(data.targetRaise, data.currency)} />
        <StatBlock label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
        <StatBlock label="Progress" value={formatPercent(pctRaised)} />
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="financials">Financials</TabsTrigger>
          <TabsTrigger value="offering">Offering</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="structure">Investment Structure</TabsTrigger>
          <TabsTrigger value="token">Token Information</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <Card>
            <CardContent>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Term label="Property" value={asset.data?.name} />
                <Term label="Valuation" value={asset.data ? formatCurrency(asset.data.valuation, asset.data.currency) : undefined} />
                <Term label="Asset Type" value={asset.data?.type.replaceAll("_", " ")} />
                <Term label="Location" value={asset.data?.location} />
              </dl>
              {asset.data?.description && <p className="mt-4 text-sm text-ink-600">{asset.data.description}</p>}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="financials">
          <Card>
            <CardContent>
              <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <Term label="Offered Interest" value={formatPercent(data.offeredInterestPercentage)} />
                <Term label="Total Units" value={data.totalUnits.toLocaleString()} />
                <Term label="Units Available" value={data.unitsAvailable.toLocaleString()} />
                <Term label="Minimum Investment" value={formatCurrency(data.minimumInvestment, data.currency)} />
                <Term label="Opening Date" value={formatDate(data.openingDate)} />
                <Term label="Closing Date" value={formatDate(data.closingDate)} />
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="offering">
          <Card>
            <CardContent>
              <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <Term label="Issuer" value={legalStructure.data?.legalEntityName ?? "—"} />
                <Term label="Legal Structure" value={legalStructure.data?.entityType.replaceAll("_", " ")} />
                <Term label="Jurisdiction" value={legalStructure.data?.jurisdiction} />
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents">
          <DocumentsPanel resourceType="Offering" resourceId={offeringId} />
        </TabsContent>

        <TabsContent value="structure">
          <Card>
            <CardContent>
              <OwnershipFlowDiagram
                nodes={[
                  { label: asset.data?.name ?? "Property" },
                  { label: legalStructure.data?.legalEntityName ?? "SPV", sublabel: "Legal owner of the property" },
                  { label: "Investment Offering", emphasis: true, sublabel: data.name },
                  { label: "ERC-3643 Investment Units", sublabel: `${data.totalUnits.toLocaleString()} units` },
                  { label: "Eligible Investors" },
                ]}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="token">
          <Card>
            <CardContent>
              <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <Term label="Token Standard" value="ERC-3643" />
                <Term label="Total Units" value={data.totalUnits.toLocaleString()} />
                <Term label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
              </dl>

              {tokenDeployment?.contractAddress ? (
                <div className="mt-4 flex flex-col gap-1 rounded-lg border border-surface-border bg-surface-subtle p-4">
                  <p className="text-xs uppercase tracking-wide text-ink-500">Contract Address</p>
                  <p className="font-mono text-sm text-ink-900">{tokenDeployment.contractAddress}</p>
                  <p className="mt-2 text-xs text-ink-500">
                    Add this address as a custom token in your wallet (MetaMask: Tokens → Import tokens) to see your
                    unit balance directly there, alongside your other holdings.
                  </p>
                </div>
              ) : (
                <p className="mt-4 text-sm text-ink-500">
                  On-chain contract details become available once this offering is tokenized. Verified, eligible
                  investors only may hold these units — see Profile &amp; Verification.
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

function StatBlock({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-surface-border bg-white p-4">
      <p className="text-xs uppercase tracking-wide text-ink-500">{label}</p>
      <div className="mt-1 text-sm font-semibold text-ink-900">{value}</div>
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
