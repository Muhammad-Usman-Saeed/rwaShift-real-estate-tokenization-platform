"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Users } from "@/components/ui/icons";
import { CopyButton } from "@/components/ui/CopyButton";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { investorsApi } from "@/lib/api/investors";
import { kycApi } from "@/lib/api/kyc";
import { queryKeys } from "@/lib/api/query-keys";
import { truncateHex } from "@/lib/utils/format";

export default function AdminInvestorsPage() {
  const investors = useAuthedQuery(queryKeys.investors.list(), (t) => investorsApi.list(t));
  const kycCases = useAuthedQuery(queryKeys.kyc.cases(), (t) => kycApi.listCases(t));

  return (
    <div>
      <PageHeader
        title="Investors"
        description={
          investors.data
            ? `${investors.data.length} investor${investors.data.length === 1 ? "" : "s"} registered on the platform.`
            : "Every investor registered on the platform."
        }
      />

      {investors.isLoading && <SkeletonTable rows={5} cols={5} />}
      {investors.isError && <ErrorBanner error={investors.error} />}

      {investors.data && investors.data.length === 0 && (
        <EmptyState icon={<Users className="h-8 w-8" />} title="No investors yet" description="Investors will appear here once they onboard." />
      )}

      {investors.data && investors.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Country</TableHead>
              <TableHead>KYC Status</TableHead>
              <TableHead>Wallet</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {investors.data.map((investor) => {
              const kyc = kycCases.data?.find((c) => c.investorId === investor.id);
              return (
                <TableRow key={investor.id}>
                  <TableCell className="font-medium text-ink-900">{investor.displayName}</TableCell>
                  <TableCell>{investor.investorType}</TableCell>
                  <TableCell>{investor.countryCode}</TableCell>
                  <TableCell>
                    <StatusPill status={kyc?.status ?? "NOT_STARTED"} />
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {investor.primaryWalletAddress ? (
                      <span className="inline-flex items-center gap-1.5">
                        {truncateHex(investor.primaryWalletAddress)}
                        <CopyButton value={investor.primaryWalletAddress} label="Copy wallet address" />
                      </span>
                    ) : (
                      "—"
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
