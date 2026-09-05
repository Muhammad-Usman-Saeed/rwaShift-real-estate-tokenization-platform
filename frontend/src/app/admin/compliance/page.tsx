"use client";

import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Callout } from "@/components/ui/Callout";
import { ShieldCheck } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { investmentsApi } from "@/lib/api/investments";
import { investorsApi } from "@/lib/api/investors";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";

export default function CompliancePage() {
  const queryClient = useQueryClient();
  const { push } = useToast();

  const investments = useAuthedQuery(queryKeys.investments.forOrganization(), (t) => investmentsApi.forOrganization(t));
  const investors = useAuthedQuery(queryKeys.investors.list(), (t) => investorsApi.list(t));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));

  const confirmPayment = useAuthedMutation((token, id: string) => investmentsApi.confirmPayment(id, token), {
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.investments.forOrganization() });
      push({ title: "Payment confirmed (demo)", variant: "success" });
    },
    onError: () => push({ title: "Could not confirm payment", variant: "error" }),
  });

  const pendingPayments = investments.data?.filter((i) => i.status === "PAYMENT_PENDING") ?? [];

  return (
    <div>
      <PageHeader
        title="Compliance"
        description={
          investments.data
            ? `${pendingPayments.length} payment${pendingPayments.length === 1 ? "" : "s"} awaiting confirmation — eligibility and demo payment settlement oversight.`
            : "Eligibility and demo payment settlement oversight."
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Pending Demo Payments</CardTitle>
        </CardHeader>
        <CardContent>
          <Callout tone="warning" className="mb-4">
            DEMO SETTLEMENT — confirming here simulates a bank webhook; no real funds move in V1.
          </Callout>

          {investments.isLoading && <SkeletonText lines={3} />}
          {investments.isError && <ErrorBanner error={investments.error} />}

          {investments.data && pendingPayments.length === 0 && (
            <EmptyState icon={<ShieldCheck className="h-8 w-8" />} title="No payments awaiting confirmation" />
          )}

          {pendingPayments.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Investor</TableHead>
                  <TableHead>Offering</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pendingPayments.map((investment) => (
                  <TableRow key={investment.id}>
                    <TableCell>{investors.data?.find((i) => i.id === investment.investorId)?.displayName ?? investment.investorId}</TableCell>
                    <TableCell>{offerings.data?.find((o) => o.id === investment.offeringId)?.name ?? investment.offeringId}</TableCell>
                    <TableCell className="tabular-nums">{formatCurrency(investment.amount, investment.currency)}</TableCell>
                    <TableCell>
                      <StatusPill status={investment.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button size="sm" isLoading={confirmPayment.isPending} onClick={() => confirmPayment.mutate(investment.id)}>
                        Confirm Payment (Demo)
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
