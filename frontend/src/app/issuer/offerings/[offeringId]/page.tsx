"use client";

import Link from "next/link";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { StatusPill } from "@/components/domain/StatusPill";
import { OfferingLifecycleTimeline } from "@/components/domain/OfferingLifecycleTimeline";
import { DocumentsPanel } from "@/components/domain/DocumentsPanel";
import { RoleGate } from "@/components/layout/RoleGate";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { Users, Search, Filter, ChevronDown } from "@/components/ui/icons";
import { CopyButton } from "@/components/ui/CopyButton";
import { cn } from "@/lib/utils/cn";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { offeringsApi } from "@/lib/api/offerings";
import { ownershipApi } from "@/lib/api/ownership";
import { investmentsApi } from "@/lib/api/investments";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatPercent, truncateHex } from "@/lib/utils/format";
import { ROLES } from "@/lib/utils/constants";
import { decimalField, currencyCodeSchema, positiveIntSchema } from "@/lib/validation/common";

const editSchema = z.object({
  name: z.string().min(1, "Offering name is required").max(300),
  targetRaise: decimalField({ min: 0.01, message: "Capital to raise must be greater than zero" }),
  currency: currencyCodeSchema,
  totalUnits: positiveIntSchema,
  unitPrice: decimalField({ min: 0.0001, maxFractionDigits: 4 }),
  minimumInvestment: decimalField({ min: 0.01 }),
  offeredInterestPercentage: decimalField({ min: 0.0001, maxFractionDigits: 4 }),
  openingDate: z.string().optional(),
  closingDate: z.string().optional(),
});
type EditFormValues = z.input<typeof editSchema>;

const OWNERSHIP_STATUS_OPTIONS = [
  { value: "ALL", label: "All Statuses" },
  { value: "ELIGIBILITY_PENDING", label: "Eligibility Pending" },
  { value: "ELIGIBILITY_REJECTED", label: "Eligibility Rejected" },
  { value: "PAYMENT_PENDING", label: "Payment Pending" },
  { value: "PAYMENT_FAILED", label: "Payment Failed" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "TOKEN_ISSUANCE_PENDING", label: "Token Issuance Pending" },
  { value: "TOKEN_ISSUANCE_FAILED", label: "Token Issuance Failed" },
  { value: "SETTLED", label: "Settled" },
  { value: "CANCELLED", label: "Cancelled" },
];

const OWNERSHIP_BLOCKCHAIN_OPTIONS = [
  { value: "ALL", label: "All Blockchain Statuses" },
  { value: "NOT_STARTED", label: "Not Started" },
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "FAILED", label: "Failed" },
];

export default function OfferingDetailPage({ params }: { params: { offeringId: string } }) {
  const { offeringId } = params;
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [editing, setEditing] = useState(false);
  const [ownershipFiltersOpen, setOwnershipFiltersOpen] = useState(false);
  const [ownershipSearch, setOwnershipSearch] = useState("");
  const [ownershipStatusFilter, setOwnershipStatusFilter] = useState("ALL");
  const [ownershipBlockchainFilter, setOwnershipBlockchainFilter] = useState("ALL");
  const [unitsMin, setUnitsMin] = useState("");
  const [unitsMax, setUnitsMax] = useState("");
  const [investmentMin, setInvestmentMin] = useState("");
  const [investmentMax, setInvestmentMax] = useState("");
  const [offeringPctMin, setOfferingPctMin] = useState("");
  const [offeringPctMax, setOfferingPctMax] = useState("");

  const offering = useAuthedQuery(queryKeys.offerings.detail(offeringId), (token) => offeringsApi.get(offeringId, token));
  const ownership = useAuthedQuery(queryKeys.ownership.forOffering(offeringId), (token) => ownershipApi.forOffering(offeringId, token));
  const investments = useAuthedQuery(queryKeys.investments.forOrganization(), (token) => investmentsApi.forOrganization(token));

  const submitForReview = useAuthedMutation((token) => offeringsApi.submitForReview(offeringId, token));
  const update = useAuthedMutation((token, values: z.output<typeof editSchema>) =>
    offeringsApi.update(
      offeringId,
      {
        ...values,
        openingDate: values.openingDate ? new Date(values.openingDate).toISOString() : undefined,
        closingDate: values.closingDate ? new Date(values.closingDate).toISOString() : undefined,
      },
      token,
    ),
  );

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditFormValues>({ resolver: zodResolver(editSchema) });

  if (offering.isLoading) return <SkeletonText lines={6} />;
  if (offering.isError || !offering.data) return <ErrorBanner error={offering.error} />;

  const data = offering.data;
  const offeringInvestments = investments.data?.filter((i) => i.offeringId === offeringId) ?? [];
  const ownershipRecords = ownership.data ?? [];
  const filteredOwnershipInvestments = offeringInvestments.filter((investment) => {
    const pct = data.totalUnits > 0 ? (investment.units / data.totalUnits) * 100 : 0;
    const matchesSearch = !ownershipSearch || investment.investorId.toLowerCase().includes(ownershipSearch.toLowerCase());
    const matchesStatus = ownershipStatusFilter === "ALL" || investment.status === ownershipStatusFilter;
    const matchesBlockchain = ownershipBlockchainFilter === "ALL" || investment.tokenIssuanceStatus === ownershipBlockchainFilter;
    const matchesUnitsMin = !unitsMin || investment.units >= Number(unitsMin);
    const matchesUnitsMax = !unitsMax || investment.units <= Number(unitsMax);
    const matchesInvestmentMin = !investmentMin || investment.amount >= Number(investmentMin);
    const matchesInvestmentMax = !investmentMax || investment.amount <= Number(investmentMax);
    const matchesPctMin = !offeringPctMin || pct >= Number(offeringPctMin);
    const matchesPctMax = !offeringPctMax || pct <= Number(offeringPctMax);
    return (
      matchesSearch &&
      matchesStatus &&
      matchesBlockchain &&
      matchesUnitsMin &&
      matchesUnitsMax &&
      matchesInvestmentMin &&
      matchesInvestmentMax &&
      matchesPctMin &&
      matchesPctMax
    );
  });
  const activeOwnershipFilterCount = [
    !!ownershipSearch,
    ownershipStatusFilter !== "ALL",
    ownershipBlockchainFilter !== "ALL",
    !!unitsMin,
    !!unitsMax,
    !!investmentMin,
    !!investmentMax,
    !!offeringPctMin,
    !!offeringPctMax,
  ].filter(Boolean).length;
  const hasActiveOwnershipFilters = activeOwnershipFilterCount > 0;

  function clearOwnershipFilters() {
    setOwnershipSearch("");
    setOwnershipStatusFilter("ALL");
    setOwnershipBlockchainFilter("ALL");
    setUnitsMin("");
    setUnitsMax("");
    setInvestmentMin("");
    setInvestmentMax("");
    setOfferingPctMin("");
    setOfferingPctMax("");
  }

  function openEdit() {
    reset({
      name: data.name,
      targetRaise: data.targetRaise,
      currency: data.currency,
      totalUnits: data.totalUnits,
      unitPrice: data.unitPrice,
      minimumInvestment: data.minimumInvestment,
      offeredInterestPercentage: data.offeredInterestPercentage,
      openingDate: data.openingDate ? data.openingDate.slice(0, 10) : undefined,
      closingDate: data.closingDate ? data.closingDate.slice(0, 10) : undefined,
    });
    setEditing(true);
  }

  return (
    <div>
      <PageHeader
        title={data.name}
        breadcrumb={
          <Link href="/issuer/offerings" className="hover:underline">
            Offerings
          </Link>
        }
        description={`${formatCurrency(data.targetRaise, data.currency)} raise · ${data.totalUnits.toLocaleString()} units at ${formatCurrency(data.unitPrice, data.currency)}/unit`}
        actions={
          <>
            <RoleGate allow={[ROLES.ISSUER_ADMIN, ROLES.ORGANIZATION_ADMIN]}>
              {data.status === "DRAFT" && (
                <Button variant="secondary" onClick={openEdit}>
                  Edit
                </Button>
              )}
            </RoleGate>
            {data.status === "DRAFT" && (
              <Button
                isLoading={submitForReview.isPending}
                onClick={() =>
                  submitForReview.mutate(undefined, {
                    onSuccess: () => {
                      queryClient.invalidateQueries({ queryKey: queryKeys.offerings.detail(offeringId) });
                      push({ title: "Submitted for review", description: "Awaiting rwaShift review.", variant: "success" });
                    },
                  })
                }
              >
                Submit for Review
              </Button>
            )}
            {data.status === "APPROVED" && (
              <Link href={`/issuer/offerings/${offeringId}/tokenize`}>
                <Button>Tokenize Offering</Button>
              </Link>
            )}
          </>
        }
      />

      {submitForReview.isError && <ErrorBanner error={submitForReview.error} className="mb-4" />}

      {data.status === "UNDER_REVIEW" && (
        <div className="mb-6 rounded-md border border-info-500/30 bg-info-50 px-4 py-3 text-sm text-info-700">
          Awaiting rwaShift Review — a platform administrator or compliance officer will review this offering shortly.
        </div>
      )}

      <Card className="mb-6">
        <CardContent>
          <OfferingLifecycleTimeline status={data.status} />
        </CardContent>
      </Card>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="ownership">Ownership Register</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <Card>
            <CardHeader>
              <CardTitle>Offering Terms</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid grid-cols-2 gap-6 sm:grid-cols-4">
                <Term label="Target Raise" value={formatCurrency(data.targetRaise, data.currency)} />
                <Term label="Offered Interest" value={formatPercent(data.offeredInterestPercentage)} />
                <Term label="Total Units" value={data.totalUnits.toLocaleString()} />
                <Term label="Unit Price" value={formatCurrency(data.unitPrice, data.currency, { maximumFractionDigits: 4 })} />
                <Term label="Minimum Investment" value={formatCurrency(data.minimumInvestment, data.currency)} />
                <Term label="Units Issued" value={data.unitsIssued.toLocaleString()} />
                <Term label="Units Available" value={data.unitsAvailable.toLocaleString()} />
                <Term label="Status" value={<StatusPill status={data.status} />} />
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="ownership">
          <Card>
            <CardHeader>
              <CardTitle>Capital Raised &amp; Investor Register</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="mb-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                <Term label="Capital Raised" value={formatCurrency(offeringInvestments.reduce((sum, i) => sum + i.amount, 0), data.currency)} />
                <Term label="Units Issued" value={data.unitsIssued.toLocaleString()} />
                <Term label="Units Available" value={data.unitsAvailable.toLocaleString()} />
                <Term label="Investor Count" value={new Set(offeringInvestments.map((i) => i.investorId)).size} />
              </div>

              {offeringInvestments.length === 0 ? (
                <EmptyState icon={<Users className="h-8 w-8" />} title="No investors yet" />
              ) : (
                <>
                  <div className="mb-4 flex flex-col gap-3 rounded-lg border border-surface-border bg-surface-subtle p-3">
                    <div className="flex gap-3">
                      <div className="relative flex-1">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
                        <Input
                          placeholder="Search by investor ID…"
                          value={ownershipSearch}
                          onChange={(e) => setOwnershipSearch(e.target.value)}
                          className="bg-white pl-9"
                          aria-label="Search investor register"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setOwnershipFiltersOpen((v) => !v)}
                        className={cn(
                          "flex h-10 shrink-0 items-center gap-1.5 rounded-md border px-3 text-sm font-medium",
                          ownershipFiltersOpen ? "border-brand-600 bg-brand-50 text-brand-700" : "border-surface-border bg-white text-ink-700 hover:bg-surface-subtle",
                        )}
                      >
                        <Filter className="h-4 w-4" />
                        Filters
                        {activeOwnershipFilterCount > 0 && (
                          <span className="rounded-full bg-brand-600 px-1.5 text-[10px] font-semibold leading-4 text-white">
                            {activeOwnershipFilterCount}
                          </span>
                        )}
                        <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", ownershipFiltersOpen && "rotate-180")} />
                      </button>
                    </div>

                    {ownershipFiltersOpen && (
                      <div className="flex flex-col gap-3 border-t border-surface-border pt-3">
                        <div className="grid grid-cols-2 gap-3 sm:grid-cols-2">
                          <Select
                            value={ownershipStatusFilter}
                            onValueChange={setOwnershipStatusFilter}
                            options={OWNERSHIP_STATUS_OPTIONS}
                          />
                          <Select
                            value={ownershipBlockchainFilter}
                            onValueChange={setOwnershipBlockchainFilter}
                            options={OWNERSHIP_BLOCKCHAIN_OPTIONS}
                          />
                        </div>

                        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                          <RangeFilter label="Units" min={unitsMin} max={unitsMax} onMinChange={setUnitsMin} onMaxChange={setUnitsMax} />
                          <RangeFilter
                            label="Investment"
                            min={investmentMin}
                            max={investmentMax}
                            onMinChange={setInvestmentMin}
                            onMaxChange={setInvestmentMax}
                          />
                          <RangeFilter
                            label="Offering %"
                            min={offeringPctMin}
                            max={offeringPctMax}
                            onMinChange={setOfferingPctMin}
                            onMaxChange={setOfferingPctMax}
                          />
                        </div>

                        {hasActiveOwnershipFilters && (
                          <div className="flex justify-end">
                            <button type="button" onClick={clearOwnershipFilters} className="text-xs font-medium text-brand-700 hover:underline">
                              Clear Filters
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {filteredOwnershipInvestments.length === 0 ? (
                    <EmptyState icon={<Users className="h-8 w-8" />} title="No matching investments" />
                  ) : (
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Investor</TableHead>
                          <TableHead>Units</TableHead>
                          <TableHead>Investment</TableHead>
                          <TableHead>Offering %</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead>Blockchain</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {filteredOwnershipInvestments.map((investment) => {
                          const onChain = ownershipRecords.find((r) => r.investorId === investment.investorId);
                          const pct = data.totalUnits > 0 ? (investment.units / data.totalUnits) * 100 : 0;
                          return (
                            <TableRow key={investment.id}>
                              <TableCell className="font-mono text-xs">{truncateHex(investment.investorId, 8, 4)}</TableCell>
                              <TableCell className="tabular-nums">{investment.units.toLocaleString()}</TableCell>
                              <TableCell className="tabular-nums">{formatCurrency(investment.amount, investment.currency)}</TableCell>
                              <TableCell className="tabular-nums">{formatPercent(pct, 2)}</TableCell>
                              <TableCell>
                                <StatusPill status={investment.status} />
                              </TableCell>
                              <TableCell>
                                <StatusPill status={investment.tokenIssuanceStatus} />
                                {onChain && (
                                  <p className="mt-1 flex items-center gap-1 font-mono text-[10px] text-ink-400">
                                    {truncateHex(onChain.walletAddress)}
                                    <CopyButton value={onChain.walletAddress} label="Copy wallet address" className="h-4 w-4" />
                                  </p>
                                )}
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  )}
                </>
              )}
              <p className="mt-3 text-xs text-ink-500">
                Full KYC and eligibility detail is available to compliance reviewers in the Platform Admin portal.
              </p>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents">
          <DocumentsPanel resourceType="Offering" resourceId={offeringId} />
        </TabsContent>
      </Tabs>

      <Modal open={editing} onOpenChange={setEditing} title="Edit Offering" description="Update this offering's terms.">
        {update.isError && <ErrorBanner error={update.error} className="mb-4" />}
        <form
          className="flex flex-col gap-4"
          onSubmit={handleSubmit((values) => {
            const parsed = editSchema.parse(values);
            update.mutate(parsed, {
              onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: queryKeys.offerings.detail(offeringId) });
                push({ title: "Offering updated", variant: "success" });
                setEditing(false);
              },
              onError: () => push({ title: "Could not update offering", variant: "error" }),
            });
          })}
        >
          <Field label="Offering Name" htmlFor="edit-name" required error={errors.name?.message}>
            <Input id="edit-name" {...register("name")} invalid={!!errors.name} />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Target Raise" htmlFor="edit-targetRaise" required error={errors.targetRaise?.message}>
              <Input id="edit-targetRaise" type="number" step="0.01" min="0.01" {...register("targetRaise")} invalid={!!errors.targetRaise} />
            </Field>
            <Field label="Currency" htmlFor="edit-currency" required error={errors.currency?.message}>
              <Input id="edit-currency" maxLength={3} className="uppercase" {...register("currency")} invalid={!!errors.currency} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Total Units" htmlFor="edit-totalUnits" required error={errors.totalUnits?.message}>
              <Input id="edit-totalUnits" type="number" step="1" min="1" {...register("totalUnits")} invalid={!!errors.totalUnits} />
            </Field>
            <Field label="Unit Price" htmlFor="edit-unitPrice" required error={errors.unitPrice?.message}>
              <Input id="edit-unitPrice" type="number" step="0.0001" min="0.0001" {...register("unitPrice")} invalid={!!errors.unitPrice} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Minimum Investment" htmlFor="edit-minimumInvestment" required error={errors.minimumInvestment?.message}>
              <Input id="edit-minimumInvestment" type="number" step="0.01" min="0.01" {...register("minimumInvestment")} invalid={!!errors.minimumInvestment} />
            </Field>
            <Field label="Offered Interest %" htmlFor="edit-offeredInterestPercentage" required error={errors.offeredInterestPercentage?.message}>
              <Input id="edit-offeredInterestPercentage" type="number" step="0.0001" min="0.0001" {...register("offeredInterestPercentage")} invalid={!!errors.offeredInterestPercentage} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Opening Date" htmlFor="edit-openingDate" hint="Optional">
              <Input id="edit-openingDate" type="date" {...register("openingDate")} />
            </Field>
            <Field label="Closing Date" htmlFor="edit-closingDate" hint="Optional">
              <Input id="edit-closingDate" type="date" {...register("closingDate")} />
            </Field>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={update.isPending}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function RangeFilter({
  label,
  min,
  max,
  onMinChange,
  onMaxChange,
}: {
  label: string;
  min: string;
  max: string;
  onMinChange: (value: string) => void;
  onMaxChange: (value: string) => void;
}) {
  return (
    <div>
      <p className="mb-1 text-xs font-medium text-ink-600">{label}</p>
      <div className="flex items-center gap-2">
        <Input
          type="number"
          placeholder="Min"
          value={min}
          onChange={(e) => onMinChange(e.target.value)}
          className="bg-white"
          aria-label={`${label} minimum`}
        />
        <span className="text-ink-400">–</span>
        <Input
          type="number"
          placeholder="Max"
          value={max}
          onChange={(e) => onMaxChange(e.target.value)}
          className="bg-white"
          aria-label={`${label} maximum`}
        />
      </div>
    </div>
  );
}

function Term({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-500">{label}</dt>
      <dd className="mt-1 text-sm font-semibold text-ink-900">{value}</dd>
    </div>
  );
}
