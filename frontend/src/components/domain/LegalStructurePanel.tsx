"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { StatusPill } from "@/components/domain/StatusPill";
import { OwnershipFlowDiagram } from "@/components/domain/OwnershipFlowDiagram";
import { DocumentsPanel } from "@/components/domain/DocumentsPanel";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { legalStructuresApi } from "@/lib/api/legal-structures";
import { queryKeys } from "@/lib/api/query-keys";
import type { InvestmentInstrumentType, LegalEntityType, RelationshipToAsset } from "@/lib/api/types";

const ENTITY_TYPE_OPTIONS = [
  { value: "SPV", label: "Special Purpose Vehicle (SPV)" },
  { value: "TRUST", label: "Trust" },
  { value: "FUND", label: "Fund" },
  { value: "DIRECT_HOLDING", label: "Direct Holding" },
];
const RELATIONSHIP_OPTIONS = [
  { value: "FULL_OWNER", label: "Full Owner" },
  { value: "MAJORITY_OWNER", label: "Majority Owner" },
  { value: "MINORITY_OWNER", label: "Minority Owner" },
  { value: "LEASEHOLD", label: "Leasehold" },
];
const INSTRUMENT_OPTIONS = [
  { value: "EQUITY_UNITS", label: "Equity Units" },
  { value: "DEBT_NOTE", label: "Debt Note" },
  { value: "PREFERRED_UNITS", label: "Preferred Units" },
];

const schema = z.object({
  legalEntityName: z.string().min(1, "Legal entity name is required").max(300),
  entityType: z.enum(["SPV", "TRUST", "FUND", "DIRECT_HOLDING"]),
  jurisdiction: z.string().min(1, "Jurisdiction is required").max(100),
  registrationNumber: z.string().max(100).optional(),
  relationshipToAsset: z.enum(["FULL_OWNER", "MAJORITY_OWNER", "MINORITY_OWNER", "LEASEHOLD"]),
  investmentInstrumentType: z.enum(["EQUITY_UNITS", "DEBT_NOTE", "PREFERRED_UNITS"]),
});
type FormValues = z.infer<typeof schema>;

export function LegalStructurePanel({ assetId, assetName }: { assetId: string; assetName: string }) {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [editing, setEditing] = useState(false);

  const legalStructures = useAuthedQuery(queryKeys.legalStructures.forAsset(assetId), (token) => legalStructuresApi.list(token));
  const structure = legalStructures.data?.find((ls) => ls.assetId === assetId);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      legalEntityName: `${assetName} SPV Ltd.`,
      entityType: "SPV",
      jurisdiction: "DIFC, UAE",
      relationshipToAsset: "FULL_OWNER",
      investmentInstrumentType: "EQUITY_UNITS",
    },
  });

  const createLegalStructure = useAuthedMutation((token, values: FormValues) =>
    legalStructuresApi.create({ assetId, ...values }, token),
  );
  const updateLegalStructure = useAuthedMutation((token, values: FormValues) =>
    legalStructuresApi.update(structure!.id, values, token),
  );
  const activate = useAuthedMutation((token, id: string) => legalStructuresApi.activate(id, token));

  function openEdit() {
    if (!structure) return;
    reset({
      legalEntityName: structure.legalEntityName,
      entityType: structure.entityType,
      jurisdiction: structure.jurisdiction,
      registrationNumber: structure.registrationNumber ?? undefined,
      relationshipToAsset: structure.relationshipToAsset,
      investmentInstrumentType: structure.investmentInstrumentType,
    });
    setEditing(true);
  }

  if (legalStructures.isLoading) return null;

  if (structure && editing) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Edit Legal Structure</CardTitle>
        </CardHeader>
        <CardContent>
          {updateLegalStructure.isError && <ErrorBanner error={updateLegalStructure.error} className="mb-4" />}
          <form
            className="flex flex-col gap-5"
            onSubmit={handleSubmit((values) =>
              updateLegalStructure.mutate(values, {
                onSuccess: () => {
                  queryClient.invalidateQueries({ queryKey: queryKeys.legalStructures.forAsset(assetId) });
                  push({ title: "Legal structure updated", variant: "success" });
                  setEditing(false);
                },
                onError: () => push({ title: "Could not update legal structure", variant: "error" }),
              }),
            )}
          >
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Legal Entity Name" htmlFor="legalEntityName" required error={errors.legalEntityName?.message}>
                <Input id="legalEntityName" {...register("legalEntityName")} invalid={!!errors.legalEntityName} />
              </Field>
              <Field label="Ownership Vehicle" htmlFor="entityType" required>
                <Controller
                  control={control}
                  name="entityType"
                  render={({ field }) => (
                    <Select id="entityType" value={field.value} onValueChange={field.onChange} options={ENTITY_TYPE_OPTIONS as { value: LegalEntityType; label: string }[]} />
                  )}
                />
              </Field>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Jurisdiction" htmlFor="jurisdiction" required error={errors.jurisdiction?.message}>
                <Input id="jurisdiction" {...register("jurisdiction")} invalid={!!errors.jurisdiction} />
              </Field>
              <Field label="Registration Number" htmlFor="registrationNumber" hint="Optional">
                <Input id="registrationNumber" {...register("registrationNumber")} />
              </Field>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Relationship to Asset" htmlFor="relationshipToAsset" required>
                <Controller
                  control={control}
                  name="relationshipToAsset"
                  render={({ field }) => (
                    <Select id="relationshipToAsset" value={field.value} onValueChange={field.onChange} options={RELATIONSHIP_OPTIONS as { value: RelationshipToAsset; label: string }[]} />
                  )}
                />
              </Field>
              <Field label="Investment Instrument" htmlFor="investmentInstrumentType" required>
                <Controller
                  control={control}
                  name="investmentInstrumentType"
                  render={({ field }) => (
                    <Select id="investmentInstrumentType" value={field.value} onValueChange={field.onChange} options={INSTRUMENT_OPTIONS as { value: InvestmentInstrumentType; label: string }[]} />
                  )}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={updateLegalStructure.isPending}>
                Save Changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    );
  }

  if (!structure) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Establish Legal Structure</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-5 text-sm text-ink-500">
            Every offering must be issued through a legal structure that holds — not the physical asset directly. This is
            the ownership vehicle investors will hold units in.
          </p>
          {createLegalStructure.isError && <ErrorBanner error={createLegalStructure.error} className="mb-4" />}
          <form
            className="flex flex-col gap-5"
            onSubmit={handleSubmit((values) =>
              createLegalStructure.mutate(values, {
                onSuccess: () => {
                  queryClient.invalidateQueries({ queryKey: queryKeys.legalStructures.forAsset(assetId) });
                  push({ title: "Legal structure created", variant: "success" });
                },
                onError: () => push({ title: "Could not create legal structure", variant: "error" }),
              }),
            )}
          >
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Legal Entity Name" htmlFor="legalEntityName" required error={errors.legalEntityName?.message}>
                <Input id="legalEntityName" {...register("legalEntityName")} invalid={!!errors.legalEntityName} />
              </Field>
              <Field label="Ownership Vehicle" htmlFor="entityType" required>
                <Controller
                  control={control}
                  name="entityType"
                  render={({ field }) => (
                    <Select id="entityType" value={field.value} onValueChange={field.onChange} options={ENTITY_TYPE_OPTIONS as { value: LegalEntityType; label: string }[]} />
                  )}
                />
              </Field>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Jurisdiction" htmlFor="jurisdiction" required error={errors.jurisdiction?.message}>
                <Input id="jurisdiction" {...register("jurisdiction")} invalid={!!errors.jurisdiction} />
              </Field>
              <Field label="Registration Number" htmlFor="registrationNumber" hint="Optional">
                <Input id="registrationNumber" {...register("registrationNumber")} />
              </Field>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Relationship to Asset" htmlFor="relationshipToAsset" required>
                <Controller
                  control={control}
                  name="relationshipToAsset"
                  render={({ field }) => (
                    <Select id="relationshipToAsset" value={field.value} onValueChange={field.onChange} options={RELATIONSHIP_OPTIONS as { value: RelationshipToAsset; label: string }[]} />
                  )}
                />
              </Field>
              <Field label="Investment Instrument" htmlFor="investmentInstrumentType" required>
                <Controller
                  control={control}
                  name="investmentInstrumentType"
                  render={({ field }) => (
                    <Select id="investmentInstrumentType" value={field.value} onValueChange={field.onChange} options={INSTRUMENT_OPTIONS as { value: InvestmentInstrumentType; label: string }[]} />
                  )}
                />
              </Field>
            </div>
            <div className="flex justify-end">
              <Button type="submit" isLoading={createLegalStructure.isPending}>
                Create Legal Structure
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle>{structure.legalEntityName}</CardTitle>
          <div className="flex items-center gap-2">
            <StatusPill status={structure.status} />
            {structure.status === "DRAFT" && (
              <Button size="sm" variant="secondary" onClick={openEdit}>
                Edit
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="flex flex-col gap-6">
          <OwnershipFlowDiagram
            nodes={[
              { label: assetName, sublabel: "Physical property" },
              { label: structure.legalEntityName, sublabel: `${structure.entityType} · ${structure.jurisdiction}`, emphasis: true },
              { label: "Investment Offering", sublabel: "Issued to eligible investors" },
            ]}
          />
          <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3">
            <div>
              <dt className="text-xs uppercase text-ink-500">Entity Type</dt>
              <dd className="mt-0.5 text-ink-800">{structure.entityType.replaceAll("_", " ")}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-ink-500">Relationship</dt>
              <dd className="mt-0.5 text-ink-800">{structure.relationshipToAsset.replaceAll("_", " ")}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-ink-500">Instrument</dt>
              <dd className="mt-0.5 text-ink-800">{structure.investmentInstrumentType.replaceAll("_", " ")}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase text-ink-500">Registration No.</dt>
              <dd className="mt-0.5 text-ink-800">{structure.registrationNumber || "—"}</dd>
            </div>
          </dl>
          {structure.status === "DRAFT" && (
            <div>
              {activate.isError && <ErrorBanner error={activate.error} className="mb-3" />}
              <Button
                isLoading={activate.isPending}
                onClick={() =>
                  activate.mutate(structure.id, {
                    onSuccess: () => {
                      queryClient.invalidateQueries({ queryKey: queryKeys.legalStructures.forAsset(assetId) });
                      push({ title: "Legal structure activated", variant: "success" });
                    },
                  })
                }
              >
                Activate Legal Structure
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <div>
        <h3 className="mb-3 text-sm font-semibold text-ink-800">Supporting Documents</h3>
        <DocumentsPanel resourceType="LegalStructure" resourceId={structure.id} />
      </div>
    </div>
  );
}
