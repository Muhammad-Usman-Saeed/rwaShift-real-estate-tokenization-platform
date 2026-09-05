"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { StatusPill } from "@/components/domain/StatusPill";
import { Callout } from "@/components/ui/Callout";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useCurrentUser } from "@/lib/auth/session";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDate } from "@/lib/utils/format";

export default function IssuerSettingsPage() {
  const { organizationId, roles, userId } = useCurrentUser();
  const organization = useAuthedQuery(
    queryKeys.organizations.detail(organizationId ?? ""),
    (token) => organizationsApi.get(organizationId as string, token),
    { enabled: !!organizationId },
  );

  return (
    <div className="max-w-2xl">
      <PageHeader title="Settings" description="Organization and account information." />

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Organization</CardTitle>
          </CardHeader>
          <CardContent>
            {organization.isLoading && <SkeletonText lines={3} />}
            {organization.isError && <ErrorBanner error={organization.error} />}
            {organization.data && (
              <dl className="grid grid-cols-2 gap-4 text-sm">
                <Term label="Legal Name" value={organization.data.legalName} />
                <Term label="Display Name" value={organization.data.displayName} />
                <Term label="Country" value={organization.data.countryCode} />
                <Term label="Status" value={<StatusPill status={organization.data.status} />} />
                <Term label="Registered" value={formatDate(organization.data.createdAt)} />
              </dl>
            )}
            <Callout className="mt-4">Organization details are managed by rwaShift platform administrators.</Callout>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Your Account</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <Term label="User ID" value={userId} />
              <Term label="Roles" value={roles.join(", ")} />
            </dl>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Term({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-500">{label}</dt>
      <dd className="mt-1 font-medium text-ink-900">{value}</dd>
    </div>
  );
}
