"use client";

import { Suspense, useEffect, useMemo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useForm, Controller, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Callout } from "@/components/ui/Callout";
import { ArrowDown } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { assetsApi } from "@/lib/api/assets";
import { legalStructuresApi } from "@/lib/api/legal-structures";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { decimalField, currencyCodeSchema, positiveIntSchema } from "@/lib/validation/common";
import { formatCompactCurrency, formatCurrency, formatNumber, formatPercent } from "@/lib/utils/format";

const schema = z.object({
  assetId: z.string().min(1, "Select an asset"),
  legalStructureId: z.string().min(1, "Select a legal structure"),
  name: z.string().min(1, "Offering name is required").max(300),
  targetRaise: decimalField({ min: 0.01, message: "Capital to raise must be greater than zero" }),
  currency: currencyCodeSchema,
  totalUnits: positiveIntSchema,
  minimumInvestment: decimalField({ min: 0.01 }),
  offeredInterestPercentage: decimalField({ min: 0.0001, maxFractionDigits: 4 }),
  openingDate: z.string().optional(),
  closingDate: z.string().optional(),
});
type FormValues = z.input<typeof schema>;

export default function CreateOfferingPage() {
  return (
    <Suspense>
      <CreateOfferingForm />
    </Suspense>
  );
}

function CreateOfferingForm() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { push } = useToast();
  const searchParams = useSearchParams();
  const preselectedAssetId = searchParams.get("assetId") ?? undefined;

  const assets = useAuthedQuery(queryKeys.assets.list(), (token) => assetsApi.list(token));
  const legalStructures = useAuthedQuery(queryKeys.legalStructures.list(), (token) => legalStructuresApi.list(token));

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      assetId: preselectedAssetId ?? "",
      legalStructureId: "",
      name: "",
      targetRaise: 2_000_000,
      currency: "USD",
      totalUnits: 20_000,
      minimumInvestment: 5_000,
      offeredInterestPercentage: 20,
    },
  });

  const assetId = useWatch({ control, name: "assetId" });
  const targetRaise = useWatch({ control, name: "targetRaise" });
  const totalUnits = useWatch({ control, name: "totalUnits" });

  const selectedAsset = assets.data?.find((a) => a.id === assetId);
  const assetLegalStructures = useMemo(
    () => legalStructures.data?.filter((ls) => ls.assetId === assetId) ?? [],
    [legalStructures.data, assetId],
  );

  useEffect(() => {
    if (selectedAsset) {
      setValue("name", `${selectedAsset.name} SPV Units`);
      setValue("currency", selectedAsset.currency);
    }
  }, [selectedAsset, setValue]);

  useEffect(() => {
    if (assetLegalStructures.length === 1) {
      setValue("legalStructureId", assetLegalStructures[0]!.id);
    }
  }, [assetLegalStructures, setValue]);

  const raiseNum = Number(targetRaise) || 0;
  const unitsNum = Number(totalUnits) || 0;
  const derivedUnitPrice = unitsNum > 0 ? raiseNum / unitsNum : 0;

  const createOffering = useAuthedMutation((token, values: z.output<typeof schema>) =>
    offeringsApi.create(
      {
        assetId: values.assetId,
        legalStructureId: values.legalStructureId,
        name: values.name,
        targetRaise: values.targetRaise,
        currency: values.currency,
        totalUnits: values.totalUnits,
        unitPrice: Math.round(derivedUnitPrice * 10000) / 10000,
        minimumInvestment: values.minimumInvestment,
        offeredInterestPercentage: values.offeredInterestPercentage,
        openingDate: values.openingDate ? new Date(values.openingDate).toISOString() : undefined,
        closingDate: values.closingDate ? new Date(values.closingDate).toISOString() : undefined,
      },
      token,
    ),
  );

  function onSubmit(values: FormValues) {
    const parsed = schema.parse(values);
    createOffering.mutate(parsed, {
      onSuccess: (offering) => {
        queryClient.invalidateQueries({ queryKey: queryKeys.offerings.list() });
        push({ title: "Offering created", description: offering.name, variant: "success" });
        router.push(`/issuer/offerings/${offering.id}`);
      },
      onError: () => push({ title: "Could not create offering", variant: "error" }),
    });
  }

  const assetOptions = (assets.data ?? []).map((a) => ({ value: a.id, label: `${a.name} — ${formatCompactCurrency(a.valuation, a.currency)}` }));
  const legalStructureOptions = assetLegalStructures.map((ls) => ({ value: ls.id, label: ls.legalEntityName }));

  return (
    <div>
      <PageHeader title="Create Offering" description="Structure an investment offering issued against an asset's legal structure." />

      {createOffering.isError && <ErrorBanner error={createOffering.error} className="mb-4" />}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_22rem]">
        <Card>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
              <Field label="Asset" htmlFor="assetId" required error={errors.assetId?.message}>
                <Controller
                  control={control}
                  name="assetId"
                  render={({ field }) => (
                    <Select id="assetId" value={field.value} onValueChange={field.onChange} options={assetOptions} placeholder="Select an asset" invalid={!!errors.assetId} />
                  )}
                />
              </Field>

              {assetId && assetLegalStructures.length === 0 && (
                <Callout tone="warning">This asset has no legal structure yet. Create one from the asset&apos;s Legal Structure tab first.</Callout>
              )}

              {assetId && assetLegalStructures.length > 0 && (
                <Field label="Legal Structure" htmlFor="legalStructureId" required error={errors.legalStructureId?.message}>
                  <Controller
                    control={control}
                    name="legalStructureId"
                    render={({ field }) => (
                      <Select id="legalStructureId" value={field.value} onValueChange={field.onChange} options={legalStructureOptions} invalid={!!errors.legalStructureId} />
                    )}
                  />
                </Field>
              )}

              <Field label="Offering Name" htmlFor="name" required error={errors.name?.message}>
                <Input id="name" {...register("name")} invalid={!!errors.name} />
              </Field>

              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <Field label="Capital to Raise" htmlFor="targetRaise" required error={errors.targetRaise?.message}>
                  <Input id="targetRaise" type="number" step="0.01" {...register("targetRaise")} invalid={!!errors.targetRaise} />
                </Field>
                <Field label="Currency" htmlFor="currency" required error={errors.currency?.message}>
                  <Input id="currency" maxLength={3} {...register("currency")} className="uppercase" invalid={!!errors.currency} />
                </Field>
              </div>

              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <Field label="Total Investment Units" htmlFor="totalUnits" required error={errors.totalUnits?.message}>
                  <Input id="totalUnits" type="number" step="1" min="1" {...register("totalUnits")} invalid={!!errors.totalUnits} />
                </Field>
                <Field label="Offered Interest %" htmlFor="offeredInterestPercentage" required error={errors.offeredInterestPercentage?.message}>
                  <Input id="offeredInterestPercentage" type="number" step="0.0001" {...register("offeredInterestPercentage")} invalid={!!errors.offeredInterestPercentage} />
                </Field>
              </div>

              <Field label="Minimum Investment" htmlFor="minimumInvestment" required error={errors.minimumInvestment?.message}>
                <Input id="minimumInvestment" type="number" step="0.01" {...register("minimumInvestment")} invalid={!!errors.minimumInvestment} />
              </Field>

              <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <Field label="Opening Date" htmlFor="openingDate" hint="Optional">
                  <Input id="openingDate" type="date" {...register("openingDate")} />
                </Field>
                <Field label="Closing Date" htmlFor="closingDate" hint="Optional">
                  <Input id="closingDate" type="date" {...register("closingDate")} />
                </Field>
              </div>

              <div className="flex justify-end gap-2">
                <Button type="button" variant="secondary" onClick={() => router.back()}>
                  Cancel
                </Button>
                <Button type="submit" isLoading={createOffering.isPending} disabled={!assetId || assetLegalStructures.length === 0}>
                  Create Offering
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <div className="lg:sticky lg:top-6 lg:self-start">
          <Card>
            <CardHeader>
              <CardTitle>Offering Structure</CardTitle>
            </CardHeader>
            <CardContent>
              <OfferingCalculationDiagram
                propertyValue={selectedAsset?.valuation}
                currency={selectedAsset?.currency ?? "USD"}
                raise={raiseNum}
                interestPct={Number((useWatch({ control, name: "offeredInterestPercentage" }) as number) || 0)}
                units={unitsNum}
                unitPrice={derivedUnitPrice}
              />
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function OfferingCalculationDiagram({
  propertyValue,
  currency,
  raise,
  interestPct,
  units,
  unitPrice,
}: {
  propertyValue?: number;
  currency: string;
  raise: number;
  interestPct: number;
  units: number;
  unitPrice: number;
}) {
  return (
    <div className="flex flex-col items-center gap-2">
      <DiagramBlock label="PROPERTY VALUE" value={propertyValue ? formatCompactCurrency(propertyValue, currency) : "—"} />
      <ArrowDown className="h-4 w-4 text-ink-300" />
      <DiagramBlock
        label="TOKENIZED OFFERING"
        value={`${formatCompactCurrency(raise, currency)} / ${formatPercent(interestPct, 1)}`}
        emphasis
      />
      <ArrowDown className="h-4 w-4 text-ink-300" />
      <DiagramBlock label="UNITS" value={`${formatNumber(units)} units`} />
      <ArrowDown className="h-4 w-4 text-ink-300" />
      <DiagramBlock label="PRICE PER UNIT" value={unitPrice > 0 ? formatCurrency(unitPrice, currency, { maximumFractionDigits: 4 }) : "—"} />
      <p className="mt-2 text-center text-xs text-ink-500">Price per unit is derived automatically: Capital to Raise ÷ Total Units.</p>
    </div>
  );
}

function DiagramBlock({ label, value, emphasis }: { label: string; value: string; emphasis?: boolean }) {
  return (
    <div
      className={
        "w-full rounded-lg border px-4 py-3 text-center " + (emphasis ? "border-gold-300 bg-gold-50" : "border-surface-border bg-surface-subtle")
      }
    >
      <p className="text-[10px] font-semibold uppercase tracking-wide text-ink-500">{label}</p>
      <p className={"mt-1 text-lg font-semibold tabular-nums " + (emphasis ? "text-gold-700" : "text-ink-900")}>{value}</p>
    </div>
  );
}
