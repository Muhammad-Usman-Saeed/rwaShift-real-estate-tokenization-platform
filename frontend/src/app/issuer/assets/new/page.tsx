"use client";

import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input, Textarea } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { assetsApi } from "@/lib/api/assets";
import { queryKeys } from "@/lib/api/query-keys";
import { useQueryClient } from "@tanstack/react-query";
import { decimalField, currencyCodeSchema } from "@/lib/validation/common";
import type { AssetType } from "@/lib/api/types";

const ASSET_TYPE_OPTIONS = [
  { value: "COMMERCIAL", label: "Commercial Real Estate" },
  { value: "RESIDENTIAL", label: "Residential" },
  { value: "INDUSTRIAL", label: "Industrial" },
  { value: "MIXED_USE", label: "Mixed Use" },
  { value: "LAND", label: "Land" },
];

const schema = z.object({
  name: z.string().min(1, "Asset name is required").max(300),
  type: z.enum(["COMMERCIAL", "RESIDENTIAL", "INDUSTRIAL", "MIXED_USE", "LAND"], {
    errorMap: () => ({ message: "Select an asset type" }),
  }),
  location: z.string().min(1, "Location is required").max(500),
  valuation: decimalField({ min: 0.01, message: "Valuation must be greater than zero" }),
  currency: currencyCodeSchema,
  description: z.string().max(4000).optional(),
});

type FormValues = z.input<typeof schema>;

export default function CreateAssetPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { push } = useToast();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "Dubai Business Tower", type: "COMMERCIAL", location: "Dubai, UAE", valuation: 10_000_000, currency: "USD" },
  });

  const createAsset = useAuthedMutation((token, values: z.output<typeof schema>) =>
    assetsApi.create(
      {
        name: values.name,
        type: values.type as AssetType,
        location: values.location,
        valuation: values.valuation,
        currency: values.currency,
        description: values.description,
      },
      token,
    ),
  );

  function onSubmit(values: FormValues) {
    const parsed = schema.parse(values);
    createAsset.mutate(parsed, {
      onSuccess: (asset) => {
        queryClient.invalidateQueries({ queryKey: queryKeys.assets.list() });
        push({ title: "Asset created", description: `${asset.name} has been added.`, variant: "success" });
        router.push(`/issuer/assets/${asset.id}`);
      },
      onError: () => push({ title: "Could not create asset", variant: "error" }),
    });
  }

  return (
    <div className="max-w-2xl">
      <PageHeader title="Create Asset" description="Register a physical real estate asset your organization owns or controls." />

      {createAsset.isError && <ErrorBanner error={createAsset.error} className="mb-4" />}

      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
            <Field label="Asset Name" htmlFor="name" required error={errors.name?.message}>
              <Input id="name" {...register("name")} invalid={!!errors.name} />
            </Field>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Asset Type" htmlFor="type" required error={errors.type?.message}>
                <Controller
                  control={control}
                  name="type"
                  render={({ field }) => (
                    <Select id="type" value={field.value} onValueChange={field.onChange} options={ASSET_TYPE_OPTIONS} invalid={!!errors.type} />
                  )}
                />
              </Field>
              <Field label="Location" htmlFor="location" required error={errors.location?.message}>
                <Input id="location" {...register("location")} invalid={!!errors.location} />
              </Field>
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Valuation" htmlFor="valuation" required error={errors.valuation?.message} hint="Total appraised property value">
                <Input id="valuation" type="number" step="0.01" min="0.01" {...register("valuation")} invalid={!!errors.valuation} />
              </Field>
              <Field label="Currency" htmlFor="currency" required error={errors.currency?.message}>
                <Input id="currency" maxLength={3} {...register("currency")} invalid={!!errors.currency} className="uppercase" />
              </Field>
            </div>

            <Field label="Description" htmlFor="description" error={errors.description?.message} hint="Optional — visible to compliance reviewers">
              <Textarea id="description" rows={4} {...register("description")} />
            </Field>

            <p className="text-xs text-ink-500">
              Images and supporting documents can be attached from the asset&apos;s Documents tab once it&apos;s created.
            </p>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" isLoading={createAsset.isPending}>
                Create Asset
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
