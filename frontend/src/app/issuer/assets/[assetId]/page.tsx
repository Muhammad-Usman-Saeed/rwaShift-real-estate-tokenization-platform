"use client";

import Link from "next/link";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Input, Textarea } from "@/components/ui/Input";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { SkeletonText } from "@/components/ui/Skeleton";
import { StatusPill } from "@/components/domain/StatusPill";
import { DocumentsPanel } from "@/components/domain/DocumentsPanel";
import { LegalStructurePanel } from "@/components/domain/LegalStructurePanel";
import { RoleGate } from "@/components/layout/RoleGate";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { Layers } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { assetsApi } from "@/lib/api/assets";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";
import { ROLES } from "@/lib/utils/constants";
import { decimalField, currencyCodeSchema } from "@/lib/validation/common";

const editSchema = z.object({
  name: z.string().min(1, "Asset name is required").max(300),
  location: z.string().min(1, "Location is required").max(500),
  valuation: decimalField({ min: 0.01, message: "Valuation must be greater than zero" }),
  currency: currencyCodeSchema,
  description: z.string().max(4000).optional(),
});
type EditFormValues = z.input<typeof editSchema>;

export default function AssetDetailPage({ params }: { params: { assetId: string } }) {
  const { assetId } = params;
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [editing, setEditing] = useState(false);

  const asset = useAuthedQuery(queryKeys.assets.detail(assetId), (token) => assetsApi.get(assetId, token));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));
  const assetOfferings = offerings.data?.filter((o) => o.assetId === assetId) ?? [];

  const submitForVerification = useAuthedMutation((token) => assetsApi.submitForVerification(assetId, token));
  const verify = useAuthedMutation((token) => assetsApi.verify(assetId, token));
  const update = useAuthedMutation((token, values: z.output<typeof editSchema>) => assetsApi.update(assetId, values, token));

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<EditFormValues>({ resolver: zodResolver(editSchema) });

  if (asset.isLoading) return <SkeletonText lines={6} />;
  if (asset.isError || !asset.data) return <ErrorBanner error={asset.error} />;

  const data = asset.data;
  const activeOffering = assetOfferings.find((o) => o.status === "OPEN" || o.status === "TOKENIZING");
  const isEditable = data.status === "DRAFT" || data.status === "UNDER_VERIFICATION";

  function openEdit() {
    reset({ name: data.name, location: data.location, valuation: data.valuation, currency: data.currency, description: data.description ?? "" });
    setEditing(true);
  }

  return (
    <div>
      <PageHeader
        title={data.name}
        breadcrumb={
          <Link href="/issuer/assets" className="hover:underline">
            Assets
          </Link>
        }
        description={data.location}
        actions={
          <>
            <RoleGate allow={[ROLES.ISSUER_ADMIN, ROLES.ISSUER_OPERATOR, ROLES.ORGANIZATION_ADMIN]}>
              {isEditable && (
                <Button variant="secondary" onClick={openEdit}>
                  Edit
                </Button>
              )}
            </RoleGate>
            <RoleGate allow={[ROLES.ISSUER_ADMIN, ROLES.ORGANIZATION_ADMIN]}>
              {data.status === "DRAFT" && (
                <Button
                  variant="secondary"
                  isLoading={submitForVerification.isPending}
                  onClick={() =>
                    submitForVerification.mutate(undefined, {
                      onSuccess: () => {
                        queryClient.invalidateQueries({ queryKey: queryKeys.assets.detail(assetId) });
                        push({ title: "Submitted for verification", variant: "success" });
                      },
                    })
                  }
                >
                  Submit for Verification
                </Button>
              )}
            </RoleGate>
            <RoleGate allow={[ROLES.COMPLIANCE_OFFICER, ROLES.PLATFORM_ADMIN]}>
              {data.status === "UNDER_VERIFICATION" && (
                <Button
                  isLoading={verify.isPending}
                  onClick={() =>
                    verify.mutate(undefined, {
                      onSuccess: () => {
                        queryClient.invalidateQueries({ queryKey: queryKeys.assets.detail(assetId) });
                        push({ title: "Asset verified", variant: "success" });
                      },
                    })
                  }
                >
                  Verify Asset
                </Button>
              )}
            </RoleGate>
          </>
        }
      />

      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatBlock label="Valuation" value={formatCurrency(data.valuation, data.currency)} />
        <StatBlock label="Asset Type" value={data.type.replaceAll("_", " ")} />
        <StatBlock label="Status" value={<StatusPill status={data.status} />} />
        <StatBlock label="Active Offering" value={activeOffering ? activeOffering.name : "None"} />
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="legal-structure">Legal Structure</TabsTrigger>
          <TabsTrigger value="offerings">Offerings</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <Card>
            <CardContent>
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <dt className="text-xs uppercase text-ink-500">Description</dt>
                  <dd className="mt-1 text-sm text-ink-800">{data.description || "No description provided."}</dd>
                </div>
                <div>
                  <dt className="text-xs uppercase text-ink-500">Location</dt>
                  <dd className="mt-1 text-sm text-ink-800">{data.location}</dd>
                </div>
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents">
          <DocumentsPanel resourceType="Asset" resourceId={assetId} />
        </TabsContent>

        <TabsContent value="legal-structure">
          <LegalStructurePanel assetId={assetId} assetName={data.name} />
        </TabsContent>

        <TabsContent value="offerings">
          {assetOfferings.length === 0 ? (
            <EmptyState
              icon={<Layers className="h-8 w-8" />}
              title="No offerings yet"
              description="Once a legal structure is active, you can create an investment offering for this asset."
              action={
                <Link href={`/issuer/offerings/new?assetId=${assetId}`}>
                  <Button size="sm">Create Offering</Button>
                </Link>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Offering</TableHead>
                  <TableHead>Raise</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {assetOfferings.map((offering) => (
                  <TableRow key={offering.id}>
                    <TableCell className="font-medium text-ink-900">{offering.name}</TableCell>
                    <TableCell>{formatCurrency(offering.targetRaise, offering.currency)}</TableCell>
                    <TableCell>
                      <StatusPill status={offering.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Link href={`/issuer/offerings/${offering.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                        View
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </TabsContent>
      </Tabs>

      <Modal open={editing} onOpenChange={setEditing} title="Edit Asset" description="Update this asset's details.">
        {update.isError && <ErrorBanner error={update.error} className="mb-4" />}
        <form
          className="flex flex-col gap-4"
          onSubmit={handleSubmit((values) => {
            const parsed = editSchema.parse(values);
            update.mutate(parsed, {
              onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: queryKeys.assets.detail(assetId) });
                push({ title: "Asset updated", variant: "success" });
                setEditing(false);
              },
              onError: () => push({ title: "Could not update asset", variant: "error" }),
            });
          })}
        >
          <Field label="Asset Name" htmlFor="edit-name" required error={errors.name?.message}>
            <Input id="edit-name" {...register("name")} invalid={!!errors.name} />
          </Field>
          <Field label="Location" htmlFor="edit-location" required error={errors.location?.message}>
            <Input id="edit-location" {...register("location")} invalid={!!errors.location} />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Valuation" htmlFor="edit-valuation" required error={errors.valuation?.message}>
              <Input id="edit-valuation" type="number" step="0.01" min="0.01" {...register("valuation")} invalid={!!errors.valuation} />
            </Field>
            <Field label="Currency" htmlFor="edit-currency" required error={errors.currency?.message}>
              <Input id="edit-currency" maxLength={3} className="uppercase" {...register("currency")} invalid={!!errors.currency} />
            </Field>
          </div>
          <Field label="Description" htmlFor="edit-description" error={errors.description?.message}>
            <Textarea id="edit-description" rows={4} {...register("description")} />
          </Field>
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

function StatBlock({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-surface-border bg-white p-4">
      <p className="text-xs uppercase tracking-wide text-ink-500">{label}</p>
      <div className="mt-1 text-sm font-semibold text-ink-900">{value}</div>
    </div>
  );
}
