"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { Badge } from "@/components/ui/Badge";
import { CopyButton } from "@/components/ui/CopyButton";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Coins, Wallet } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { tokenizationApi } from "@/lib/api/tokenization";
import { offeringsApi } from "@/lib/api/offerings";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { truncateHex } from "@/lib/utils/format";

/** Below this, an org's wallet can't submit even a cheap call like `unpause` — see docs. */
const LOW_BALANCE_ETH = 0.01;

function fundingStatus(balanceEth: number) {
  if (balanceEth <= 0) return { tone: "danger" as const, label: "Needs funding" };
  if (balanceEth < LOW_BALANCE_ETH) return { tone: "warning" as const, label: "Low balance" };
  return { tone: "success" as const, label: "Funded" };
}

export default function AdminTokenizationPage() {
  const deployments = useAuthedQuery(queryKeys.tokenization.allDeployments(), (t) => tokenizationApi.allDeployments(t));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (t) => organizationsApi.list(t));
  const wallets = useAuthedQuery(queryKeys.tokenization.organizationWallets(), (t) => tokenizationApi.organizationWallets(t));

  return (
    <div>
      <PageHeader
        title="Tokenization"
        description={
          deployments.data
            ? `${deployments.data.length} contract deployment${deployments.data.length === 1 ? "" : "s"} across every offering.`
            : "ERC-3643 contract deployments across every offering."
        }
      />

      {deployments.isLoading && <SkeletonTable rows={5} cols={5} />}
      {deployments.isError && <ErrorBanner error={deployments.error} />}

      {deployments.data && deployments.data.length === 0 && (
        <EmptyState icon={<Coins className="h-8 w-8" />} title="No deployments yet" />
      )}

      {deployments.data && deployments.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Contract Type</TableHead>
              <TableHead>Network</TableHead>
              <TableHead>Contract Address</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {deployments.data.map((d, i) => (
              <TableRow key={`${d.offeringId}-${d.contractType}-${i}`}>
                <TableCell className="font-medium text-ink-900">{offerings.data?.find((o) => o.id === d.offeringId)?.name ?? d.offeringId}</TableCell>
                <TableCell>{d.contractType.replaceAll("_", " ")}</TableCell>
                <TableCell className="capitalize">{d.network}</TableCell>
                <TableCell className="font-mono text-xs">
                  {d.contractAddress ? (
                    <span className="inline-flex items-center gap-1.5">
                      {truncateHex(d.contractAddress, 10, 8)}
                      <CopyButton value={d.contractAddress} label="Copy contract address" />
                    </span>
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell>
                  <StatusPill status={d.status} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <div className="mt-10">
        <h2 className="text-lg font-semibold text-ink-900">Organization wallets</h2>
        <p className="mt-1 text-sm text-ink-500">
          Each organization&apos;s own on-chain signing wallet — platform-funded, not the organization&apos;s own. It
          needs gas to submit token operations (unpause, mint, burn, freeze) for its offerings.
        </p>

        <div className="mt-4">
          {wallets.isLoading && <SkeletonTable rows={3} cols={4} />}
          {wallets.isError && <ErrorBanner error={wallets.error} />}

          {wallets.data && wallets.data.length === 0 && (
            <EmptyState icon={<Wallet className="h-8 w-8" />} title="No organization wallets provisioned yet" />
          )}

          {wallets.data && wallets.data.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Organization</TableHead>
                  <TableHead>Wallet Address</TableHead>
                  <TableHead>Balance</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {wallets.data.map((w) => {
                  const status = fundingStatus(w.balanceEth);
                  return (
                    <TableRow key={w.organizationId}>
                      <TableCell className="font-medium text-ink-900">
                        {organizations.data?.find((o) => o.id === w.organizationId)?.displayName ?? w.organizationId}
                      </TableCell>
                      <TableCell className="font-mono text-xs">
                        <span className="inline-flex items-center gap-1.5">
                          {truncateHex(w.walletAddress, 10, 8)}
                          <CopyButton value={w.walletAddress} label="Copy wallet address" />
                        </span>
                      </TableCell>
                      <TableCell className="tabular-nums">{w.balanceEth.toLocaleString(undefined, { maximumFractionDigits: 6 })} ETH</TableCell>
                      <TableCell>
                        <Badge tone={status.tone}>{status.label}</Badge>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </div>
      </div>
    </div>
  );
}
