"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Textarea } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { ShieldCheck } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { kycApi } from "@/lib/api/kyc";
import { investorsApi } from "@/lib/api/investors";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDate } from "@/lib/utils/format";

export default function KycReviewsPage() {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [rejectTarget, setRejectTarget] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const kycCases = useAuthedQuery(queryKeys.kyc.cases(), (t) => kycApi.listCases(t));
  const investors = useAuthedQuery(queryKeys.investors.list(), (t) => investorsApi.list(t));

  const invalidate = () => queryClient.invalidateQueries({ queryKey: queryKeys.kyc.cases() });

  const startReview = useAuthedMutation((token, id: string) => kycApi.startReview(id, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Review started", variant: "info" });
    },
  });
  const verify = useAuthedMutation((token, id: string) => kycApi.verify(id, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "Investor verified", variant: "success" });
    },
  });
  const reject = useAuthedMutation((token, vars: { id: string; reason: string }) => kycApi.reject(vars.id, vars.reason, token), {
    onSuccess: () => {
      invalidate();
      push({ title: "KYC rejected", variant: "info" });
      setRejectTarget(null);
      setRejectReason("");
    },
  });

  const pending = kycCases.data?.filter((c) => c.status === "SUBMITTED" || c.status === "UNDER_REVIEW") ?? [];

  return (
    <div>
      <PageHeader
        title="KYC Reviews"
        description={kycCases.data ? `${pending.length} case${pending.length === 1 ? "" : "s"} in the investor verification queue.` : "Investor verification queue."}
      />

      {kycCases.isLoading && <SkeletonTable rows={4} cols={4} />}
      {kycCases.isError && <ErrorBanner error={kycCases.error} />}

      {kycCases.data && pending.length === 0 && (
        <EmptyState icon={<ShieldCheck className="h-8 w-8" />} title="No pending reviews" />
      )}

      {pending.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Investor</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Submitted</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {pending.map((kycCase) => {
              const investor = investors.data?.find((i) => i.id === kycCase.investorId);
              return (
                <TableRow key={kycCase.id}>
                  <TableCell className="font-medium text-ink-900">{investor?.displayName ?? kycCase.investorId}</TableCell>
                  <TableCell>
                    <StatusPill status={kycCase.status} />
                  </TableCell>
                  <TableCell>{kycCase.submittedAt ? formatDate(kycCase.submittedAt) : "—"}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      {kycCase.status === "SUBMITTED" && (
                        <Button size="sm" variant="secondary" isLoading={startReview.isPending} onClick={() => startReview.mutate(kycCase.id)}>
                          Start Review
                        </Button>
                      )}
                      {kycCase.status === "UNDER_REVIEW" && (
                        <>
                          <Button size="sm" variant="danger" onClick={() => setRejectTarget(kycCase.id)}>
                            Reject
                          </Button>
                          <Button size="sm" isLoading={verify.isPending} onClick={() => verify.mutate(kycCase.id)}>
                            Verify
                          </Button>
                        </>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}

      <Modal open={!!rejectTarget} onOpenChange={(open) => !open && setRejectTarget(null)} title="Reject KYC" description="Provide a reason for rejection.">
        {reject.isError && <ErrorBanner error={reject.error} className="mb-4" />}
        <Field label="Reason" htmlFor="reason" required>
          <Textarea id="reason" rows={3} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
        </Field>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setRejectTarget(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            disabled={!rejectReason.trim()}
            isLoading={reject.isPending}
            onClick={() => rejectTarget && reject.mutate({ id: rejectTarget, reason: rejectReason })}
          >
            Reject
          </Button>
        </div>
      </Modal>
    </div>
  );
}
