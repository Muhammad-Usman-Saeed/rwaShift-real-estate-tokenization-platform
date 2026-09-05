"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable, SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Users } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { organizationsApi } from "@/lib/api/organizations";
import { organizationUsersApi, type CreateOrganizationUserResponse } from "@/lib/api/organization-users";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDate } from "@/lib/utils/format";

const ROLE_OPTIONS = [
  { value: "ORGANIZATION_ADMIN", label: "Organization Admin" },
  { value: "ISSUER_ADMIN", label: "Issuer Admin" },
  { value: "ISSUER_OPERATOR", label: "Issuer Operator" },
  { value: "COMPLIANCE_OFFICER", label: "Compliance Officer" },
];

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  displayName: z.string().min(1, "Name is required").max(200),
  role: z.enum(["ORGANIZATION_ADMIN", "ISSUER_ADMIN", "ISSUER_OPERATOR", "COMPLIANCE_OFFICER"], {
    errorMap: () => ({ message: "Select a role" }),
  }),
});
type FormValues = z.infer<typeof schema>;

export default function AdminOrganizationDetailPage() {
  const { organizationId } = useParams<{ organizationId: string }>();
  const [modalOpen, setModalOpen] = useState(false);
  const [createdUser, setCreatedUser] = useState<CreateOrganizationUserResponse | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const organization = useAuthedQuery(queryKeys.organizations.detail(organizationId), (t) => organizationsApi.get(organizationId, t));
  const users = useAuthedQuery(queryKeys.organizations.users(organizationId), (t) => organizationUsersApi.list(organizationId, t));

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const createUser = useAuthedMutation((token, values: FormValues) => organizationUsersApi.create(organizationId, values, token));

  return (
    <div>
      <PageHeader
        title={organization.data?.displayName ?? "Organization"}
        description="Organization details and who has access to it."
        actions={<Button onClick={() => setModalOpen(true)}>Add User</Button>}
      />

      {organization.isLoading && <SkeletonText lines={3} />}
      {organization.isError && <ErrorBanner error={organization.error} />}
      {organization.data && (
        <Card className="mb-6">
          <CardContent>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div>
                <p className="text-xs uppercase tracking-wide text-ink-400">Legal Name</p>
                <p className="mt-1 text-sm font-medium text-ink-900">{organization.data.legalName}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-ink-400">Country</p>
                <p className="mt-1 text-sm font-medium text-ink-900">{organization.data.countryCode ?? "—"}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-ink-400">Status</p>
                <p className="mt-1">
                  <StatusPill status={organization.data.status} />
                </p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-ink-400">Registered</p>
                <p className="mt-1 text-sm font-medium text-ink-900">{formatDate(organization.data.createdAt)}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      <h2 className="mb-3 text-sm font-semibold text-ink-800">Users</h2>
      {users.isLoading && <SkeletonTable rows={3} cols={4} />}
      {users.isError && <ErrorBanner error={users.error} />}
      {users.data && users.data.length === 0 && (
        <EmptyState
          icon={<Users className="h-8 w-8" />}
          title="No users yet"
          description="This organization has no logins yet — add its first issuer admin to get them started."
          action={<Button onClick={() => setModalOpen(true)}>Add User</Button>}
        />
      )}
      {users.data && users.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.data.map((user) => (
              <TableRow key={user.userId}>
                <TableCell className="font-medium text-ink-900">{user.displayName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.role.replaceAll("_", " ")}</TableCell>
                <TableCell>
                  <StatusPill status={user.status} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Modal
        open={modalOpen}
        onOpenChange={(open) => {
          setModalOpen(open);
          if (!open) reset();
        }}
        title="Add User"
        description="Provision a login for this organization. A temporary password is generated for you to share with them."
      >
        {createUser.isError && <ErrorBanner error={createUser.error} className="mb-4" />}
        <form
          className="flex flex-col gap-4"
          onSubmit={handleSubmit((values) =>
            createUser.mutate(values, {
              onSuccess: (user) => {
                queryClient.invalidateQueries({ queryKey: queryKeys.organizations.users(organizationId) });
                push({ title: "User created", description: `${user.displayName} can now sign in.`, variant: "success" });
                setModalOpen(false);
                reset();
                setCreatedUser(user);
              },
              onError: () => push({ title: "Could not create user", variant: "error" }),
            }),
          )}
        >
          <Field label="Email" htmlFor="email" required error={errors.email?.message}>
            <Input id="email" type="email" {...register("email")} invalid={!!errors.email} />
          </Field>
          <Field label="Full Name" htmlFor="displayName" required error={errors.displayName?.message}>
            <Input id="displayName" {...register("displayName")} invalid={!!errors.displayName} />
          </Field>
          <Field label="Role" htmlFor="role" required error={errors.role?.message}>
            <Controller
              control={control}
              name="role"
              render={({ field }) => (
                <Select id="role" value={field.value} onValueChange={field.onChange} options={ROLE_OPTIONS} invalid={!!errors.role} />
              )}
            />
          </Field>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={createUser.isPending}>
              Create User
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={!!createdUser}
        onOpenChange={(open) => !open && setCreatedUser(null)}
        title="User Created"
        description="Share this temporary password with them now — it will not be shown again."
      >
        {createdUser && (
          <div className="flex flex-col gap-4">
            <div>
              <p className="text-xs uppercase tracking-wide text-ink-400">Email</p>
              <p className="mt-1 text-sm font-medium text-ink-900">{createdUser.email}</p>
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-ink-400">Temporary Password</p>
              <Input readOnly value={createdUser.temporaryPassword} className="mt-1 font-mono" onFocus={(e) => e.target.select()} />
            </div>
            <div className="flex justify-end">
              <Button onClick={() => setCreatedUser(null)}>Done</Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
