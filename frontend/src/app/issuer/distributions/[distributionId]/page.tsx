"use client";

import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { StatusPill } from "@/components/domain/StatusPill";
import { Callout } from "@/components/ui/Callout";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { Coins } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { distributionsApi } from "@/lib/api/distributions";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDate, formatPercent, truncateHex } from "@/lib/utils/format";

export default function DistributionDetailPage({ params }: { params: { distributionId: string } }) {
  const { distributionId } = params;
  const queryClient = useQueryClient();
  const { push } = useToast();

  const distribution = useAuthedQuery(queryKeys.distributions.detail(distributionId), (token) => distributionsApi.get(distributionId, token));
  const entitlements = useAuthedQuery(
    queryKeys.distributions.entitlements(distributionId),
    (token) => distributionsApi.entitlements(distributionId, token),
    { enabled: distribution.data?.status !== "DRAFT" },
  );

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.distributions.detail(distributionId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.distributions.entitlements(distributionId) });
  };

  const calculate = useAuthedMutation((token) => distributionsApi.calculate(distributionId, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Entitlements calculated", variant: "success" });
    },
    onError: () => push({ title: "Calculation failed", variant: "error" }),
  });
  const approve = useAuthedMutation((token) => distributionsApi.approve(distributionId, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Distribution approved", variant: "success" });
    },
  });
  const process = useAuthedMutation((token) => distributionsApi.process(distributionId, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Settlement processing started", variant: "info" });
    },
  });
  const complete = useAuthedMutation((token) => distributionsApi.complete(distributionId, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Distribution completed", variant: "success" });
    },
  });

  if (distribution.isLoading) return <SkeletonText lines={5} />;
  if (distribution.isError || !distribution.data) return <ErrorBanner error={distribution.error} />;

  const d = distribution.data;

  return (
    <div>
      <PageHeader
        title={`Distribution — ${formatCurrency(d.totalAmount, d.currency)}`}
        breadcrumb={
          <Link href="/issuer/distributions" className="hover:underline">
            Distributions
          </Link>
        }
        description={`Record date ${formatDate(d.recordDate)}`}
        actions={<StatusPill status={d.status} />}
      />

      <Callout tone="warning" className="mb-6">
        SIMULATED settlement — V1 does not move real funds.
      </Callout>

      <Card className="mb-6">
        <CardContent className="flex flex-wrap items-center gap-3">
          {[calculate, approve, process, complete].some((m) => m.isError) && (
            <ErrorBanner error={calculate.error ?? approve.error ?? process.error ?? complete.error} className="w-full" />
          )}
          {d.status === "DRAFT" && (
            <Button isLoading={calculate.isPending} onClick={() => calculate.mutate()}>
              Calculate Entitlements
            </Button>
          )}
          {d.status === "CALCULATED" && (
            <Button isLoading={approve.isPending} onClick={() => approve.mutate()}>
              Approve Distribution
            </Button>
          )}
          {d.status === "APPROVED" && (
            <Button isLoading={process.isPending} onClick={() => process.mutate()}>
              Process Settlement (Simulated)
            </Button>
          )}
          {d.status === "PROCESSING" && (
            <Button isLoading={complete.isPending} onClick={() => complete.mutate()}>
              Mark Complete
            </Button>
          )}
          {d.status === "COMPLETED" && <p className="text-sm font-medium text-success-700">Distribution complete.</p>}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Entitlements</CardTitle>
        </CardHeader>
        <CardContent>
          {d.status === "DRAFT" && <p className="text-sm text-ink-500">Calculate entitlements to see the investor breakdown.</p>}
          {entitlements.isLoading && d.status !== "DRAFT" && <SkeletonText lines={3} />}
          {entitlements.data && entitlements.data.length === 0 && d.status !== "DRAFT" && (
            <EmptyState icon={<Coins className="h-8 w-8" />} title="No entitlements" description="No outstanding units were found for this offering." />
          )}
          {entitlements.data && entitlements.data.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Investor</TableHead>
                  <TableHead>Units</TableHead>
                  <TableHead>Ownership %</TableHead>
                  <TableHead>Entitlement</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {entitlements.data.map((e, i) => {
                  const units = Number(e.unitsAtRecordDate);
                  const totalUnits = entitlements.data!.reduce((sum, x) => sum + Number(x.unitsAtRecordDate), 0);
                  const pct = totalUnits > 0 ? (units / totalUnits) * 100 : 0;
                  return (
                    <TableRow key={i}>
                      <TableCell className="font-mono text-xs">{e.investorId ? truncateHex(e.investorId, 8, 4) : truncateHex(e.walletAddress)}</TableCell>
                      <TableCell className="tabular-nums">{units.toLocaleString()}</TableCell>
                      <TableCell className="tabular-nums">{formatPercent(pct, 2)}</TableCell>
                      <TableCell className="tabular-nums font-medium">{formatCurrency(e.entitlementAmount, d.currency)}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
