"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Callout } from "@/components/ui/Callout";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { offeringsApi } from "@/lib/api/offerings";
import { distributionsApi } from "@/lib/api/distributions";
import { queryKeys } from "@/lib/api/query-keys";
import { decimalField, currencyCodeSchema } from "@/lib/validation/common";

const schema = z.object({
  offeringId: z.string().min(1, "Select an offering"),
  totalAmount: decimalField({ min: 0.01, message: "Amount must be greater than zero" }),
  currency: currencyCodeSchema,
  recordDate: z.string().min(1, "Record date is required"),
});
type FormValues = z.input<typeof schema>;

export default function CreateDistributionPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { push } = useToast();

  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));
  const fundedOfferings = offerings.data?.filter((o) => o.status === "OPEN" || o.status === "FUNDED") ?? [];

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { totalAmount: 100_000, currency: "USD", recordDate: new Date().toISOString().slice(0, 10) },
  });

  const create = useAuthedMutation((token, values: z.output<typeof schema>) =>
    distributionsApi.create(
      { offeringId: values.offeringId, totalAmount: values.totalAmount, currency: values.currency, recordDate: new Date(values.recordDate).toISOString() },
      token,
    ),
  );

  return (
    <div className="max-w-xl">
      <PageHeader title="Create Distribution" description="Distribute rental income or other proceeds to eligible investors." />

      {create.isError && <ErrorBanner error={create.error} className="mb-4" />}
      <Callout tone="warning" className="mb-4">
        SIMULATED — V1 settlement is a demo workflow, not a real payment run.
      </Callout>

      <Card>
        <CardContent>
          <form
            className="flex flex-col gap-5"
            onSubmit={handleSubmit((values) => {
              const parsed = schema.parse(values);
              create.mutate(parsed, {
                onSuccess: (distribution) => {
                  queryClient.invalidateQueries({ queryKey: queryKeys.distributions.list() });
                  push({ title: "Distribution created", variant: "success" });
                  router.push(`/issuer/distributions/${distribution.id}`);
                },
                onError: () => push({ title: "Could not create distribution", variant: "error" }),
              });
            })}
          >
            <Field label="Offering" htmlFor="offeringId" required error={errors.offeringId?.message}>
              <Controller
                control={control}
                name="offeringId"
                render={({ field }) => (
                  <Select
                    id="offeringId"
                    value={field.value}
                    onValueChange={field.onChange}
                    options={fundedOfferings.map((o) => ({ value: o.id, label: o.name }))}
                    placeholder="Select an offering"
                    invalid={!!errors.offeringId}
                  />
                )}
              />
            </Field>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Field label="Total Amount" htmlFor="totalAmount" required error={errors.totalAmount?.message}>
                <Input id="totalAmount" type="number" step="0.01" {...register("totalAmount")} invalid={!!errors.totalAmount} />
              </Field>
              <Field label="Currency" htmlFor="currency" required error={errors.currency?.message}>
                <Input id="currency" maxLength={3} className="uppercase" {...register("currency")} invalid={!!errors.currency} />
              </Field>
            </div>
            <Field label="Record Date" htmlFor="recordDate" required error={errors.recordDate?.message} hint="Ownership as of this date determines entitlements">
              <Input id="recordDate" type="date" {...register("recordDate")} invalid={!!errors.recordDate} />
            </Field>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" isLoading={create.isPending}>
                Create Distribution
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
