import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { StatusPill } from "@/components/domain/StatusPill";
import { formatCurrency, formatPercent } from "@/lib/utils/format";
import type { InvestmentResponse, OfferingResponse } from "@/lib/api/types";

const RESUMABLE_STATUSES = ["ELIGIBILITY_PENDING", "PAYMENT_PENDING"];

export function InvestmentCard({ investment, offering }: { investment: InvestmentResponse; offering?: OfferingResponse }) {
  const ownershipPct = offering && offering.totalUnits > 0 ? (investment.units / offering.totalUnits) * 100 : 0;
  const isResumable = RESUMABLE_STATUSES.includes(investment.status);

  return (
    <Card>
      <CardContent>
        <div className="flex items-start justify-between">
          <p className="text-base font-semibold text-ink-900">{offering?.name ?? investment.offeringId}</p>
          <StatusPill status={investment.status} />
        </div>
        <dl className="mt-4 grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-ink-500">Investment</dt>
          <dd className="text-right font-medium tabular-nums text-ink-800">{formatCurrency(investment.amount, investment.currency)}</dd>
          <dt className="text-ink-500">Units</dt>
          <dd className="text-right font-medium tabular-nums text-ink-800">{investment.units.toLocaleString()}</dd>
          <dt className="text-ink-500">Offering Ownership</dt>
          <dd className="text-right font-medium tabular-nums text-ink-800">{formatPercent(ownershipPct, 2)}</dd>
        </dl>
        {isResumable && (
          <Link
            href={`/investor/investments/${investment.id}`}
            className="mt-4 block rounded-md border border-brand-600 px-3 py-1.5 text-center text-xs font-medium text-brand-700 hover:bg-brand-50"
          >
            {investment.status === "PAYMENT_PENDING" ? "Resume Payment" : "View Status"}
          </Link>
        )}
      </CardContent>
    </Card>
  );
}
