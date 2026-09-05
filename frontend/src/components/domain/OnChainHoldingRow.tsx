"use client";

import { useTokenBalance } from "@/lib/wallet/useTokenBalance";
import { truncateHex } from "@/lib/utils/format";
import { NEXT_PUBLIC_CHAIN_ID, NEXT_PUBLIC_CHAIN_LABEL } from "@/lib/utils/env";
import { CopyButton } from "@/components/ui/CopyButton";
import type { OwnershipRecordResponse } from "@/lib/api/types";

export function OnChainHoldingRow({ record }: { record: OwnershipRecordResponse }) {
  const { balance, symbol, isLoading } = useTokenBalance(record.tokenAddress, record.walletAddress, NEXT_PUBLIC_CHAIN_ID);

  return (
    <div className="grid grid-cols-2 gap-3 rounded-lg border border-surface-border bg-surface-subtle p-4 text-sm sm:grid-cols-4">
      <Field label="Wallet" value={truncateHex(record.walletAddress)} mono copyValue={record.walletAddress} />
      <Field label="Token Balance" value={isLoading ? "Loading…" : balance !== undefined ? `${balance.toString()} ${symbol ?? ""}` : record.units} />
      <Field label="Contract" value={truncateHex(record.tokenAddress)} mono copyValue={record.tokenAddress} />
      <Field label="Network" value={NEXT_PUBLIC_CHAIN_LABEL} />
    </div>
  );
}

function Field({ label, value, mono, copyValue }: { label: string; value: React.ReactNode; mono?: boolean; copyValue?: string }) {
  return (
    <div>
      <p className="text-[10px] uppercase tracking-wide text-ink-500">{label}</p>
      <p className={`mt-0.5 flex items-center gap-1.5 font-medium text-ink-800 ${mono ? "font-mono text-xs" : ""}`}>
        {value}
        {copyValue && <CopyButton value={copyValue} label={`Copy ${label.toLowerCase()}`} />}
      </p>
    </div>
  );
}
