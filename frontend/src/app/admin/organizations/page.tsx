"use client";

import Link from "next/link";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Pencil, Trash, Users, Building } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDate } from "@/lib/utils/format";
import type { OrganizationResponse } from "@/lib/api/types";

const schema = z.object({
  legalName: z.string().min(1, "Legal name is required").max(300),
  displayName: z.string().min(1, "Display name is required").max(200),
  countryCode: z.string().length(2).optional(),
});
type FormValues = z.infer<typeof schema>;

const editSchema = z.object({
  legalName: z.string().min(1, "Legal name is required").max(300),
  displayName: z.string().min(1, "Display name is required").max(200),
});
type EditFormValues = z.infer<typeof editSchema>;

export default function AdminOrganizationsPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOrg, setEditingOrg] = useState<OrganizationResponse | null>(null);
  const [deletingOrg, setDeletingOrg] = useState<OrganizationResponse | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (token) => organizationsApi.list(token));

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const {
    register: registerEdit,
    handleSubmit: handleEditSubmit,
    reset: resetEdit,
    formState: { errors: editErrors },
  } = useForm<EditFormValues>({ resolver: zodResolver(editSchema) });

  const create = useAuthedMutation((token, values: FormValues) => organizationsApi.create(values, token));
  const update = useAuthedMutation((token, values: EditFormValues) =>
    organizationsApi.update(editingOrg!.id, values, token),
  );
  const remove = useAuthedMutation((token) => organizationsApi.remove(deletingOrg!.id, token));

  function openEdit(org: OrganizationResponse) {
    setEditingOrg(org);
    resetEdit({ legalName: org.legalName, displayName: org.displayName });
  }

  return (
    <div>
      <PageHeader
        title="Organizations"
        description={
          organizations.data
            ? `${organizations.data.length} issuer organization${organizations.data.length === 1 ? "" : "s"} registered on the platform.`
            : "Issuer organizations registered on the platform."
        }
        actions={<Button onClick={() => setModalOpen(true)}>Register Organization</Button>}
      />

      {organizations.isLoading && <SkeletonTable rows={4} cols={4} />}
      {organizations.isError && <ErrorBanner error={organizations.error} />}

      {organizations.data && organizations.data.length === 0 && (
        <EmptyState
          icon={<Building className="h-8 w-8" />}
          title="No organizations yet"
          description="Register the first issuer organization to get started."
          action={<Button onClick={() => setModalOpen(true)}>Register Organization</Button>}
        />
      )}

      {organizations.data && organizations.data.length > 0 && (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Organization</TableHead>
                <TableHead>Country</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Registered</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {organizations.data.map((org) => (
                <TableRow key={org.id}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-brand-700 text-sm font-semibold text-white">
                        {org.displayName.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ink-900">{org.legalName}</p>
                        <p className="truncate text-xs text-ink-500">{org.displayName}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-ink-600">{org.countryCode ?? "—"}</TableCell>
                  <TableCell>
                    <StatusPill status={org.status} />
                  </TableCell>
                  <TableCell className="text-ink-600">{formatDate(org.createdAt)}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Link href={`/admin/organizations/${org.id}`}>
                        <Button size="icon" variant="ghost" aria-label="Manage users" title="Manage Users">
                          <Users className="h-4 w-4" />
                        </Button>
                      </Link>
                      <Button size="icon" variant="ghost" aria-label="Edit organization" title="Edit" onClick={() => openEdit(org)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        className="text-danger-600 hover:bg-danger-50"
                        aria-label="Delete organization"
                        title="Delete"
                        onClick={() => setDeletingOrg(org)}
                      >
                        <Trash className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </>
      )}

      <Modal open={modalOpen} onOpenChange={setModalOpen} title="Register Organization" description="Onboard a new issuer organization.">
        {create.isError && <ErrorBanner error={create.error} className="mb-4" />}
        <form
          className="flex flex-col gap-4"
          onSubmit={handleSubmit((values) =>
            create.mutate(values, {
              onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: queryKeys.organizations.list() });
                push({ title: "Organization registered", variant: "success" });
                setModalOpen(false);
                reset();
              },
              onError: () => push({ title: "Could not register organization", variant: "error" }),
            }),
          )}
        >
          <Field label="Legal Name" htmlFor="legalName" required error={errors.legalName?.message}>
            <Input id="legalName" {...register("legalName")} invalid={!!errors.legalName} />
          </Field>
          <Field label="Display Name" htmlFor="displayName" required error={errors.displayName?.message}>
            <Input id="displayName" {...register("displayName")} invalid={!!errors.displayName} />
          </Field>
          <Field label="Country" htmlFor="countryCode" hint="2-letter ISO code, optional">
            <Input id="countryCode" maxLength={2} className="uppercase" {...register("countryCode")} />
          </Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={create.isPending}>
              Register
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={!!editingOrg}
        onOpenChange={(open) => !open && setEditingOrg(null)}
        title="Edit Organization"
        description="Update the organization's legal and display name."
      >
        {update.isError && <ErrorBanner error={update.error} className="mb-4" />}
        <form
          className="flex flex-col gap-4"
          onSubmit={handleEditSubmit((values) =>
            update.mutate(values, {
              onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: queryKeys.organizations.list() });
                push({ title: "Organization updated", variant: "success" });
                setEditingOrg(null);
              },
              onError: () => push({ title: "Could not update organization", variant: "error" }),
            }),
          )}
        >
          <Field label="Legal Name" htmlFor="edit-legalName" required error={editErrors.legalName?.message}>
            <Input id="edit-legalName" {...registerEdit("legalName")} invalid={!!editErrors.legalName} />
          </Field>
          <Field label="Display Name" htmlFor="edit-displayName" required error={editErrors.displayName?.message}>
            <Input id="edit-displayName" {...registerEdit("displayName")} invalid={!!editErrors.displayName} />
          </Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setEditingOrg(null)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={update.isPending}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={!!deletingOrg}
        onOpenChange={(open) => !open && setDeletingOrg(null)}
        title="Delete Organization"
        description={deletingOrg ? `This will permanently delete "${deletingOrg.displayName}".` : undefined}
      >
        {remove.isError && <ErrorBanner error={remove.error} className="mb-4" />}
        <p className="mb-4 text-sm text-ink-600">This action cannot be undone.</p>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={() => setDeletingOrg(null)}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="danger"
            isLoading={remove.isPending}
            onClick={() =>
              remove.mutate(undefined, {
                onSuccess: () => {
                  queryClient.invalidateQueries({ queryKey: queryKeys.organizations.list() });
                  push({ title: "Organization deleted", variant: "success" });
                  setDeletingOrg(null);
                },
              })
            }
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}
