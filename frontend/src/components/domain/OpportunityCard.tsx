import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { formatCompactCurrency, formatCurrency, formatPercent } from "@/lib/utils/format";
import type { AssetResponse, OfferingResponse } from "@/lib/api/types";

export function OpportunityCard({ offering, asset }: { offering: OfferingResponse; asset?: AssetResponse }) {
  const raisedUnits = offering.unitsIssued;
  const pctRaised = offering.totalUnits > 0 ? (raisedUnits / offering.totalUnits) * 100 : 0;

  return (
    <Card className="flex flex-col">
      <CardContent className="flex flex-1 flex-col">
        <p className="text-base font-semibold text-ink-900">{asset?.name ?? offering.name}</p>
        {asset && (
          <p className="mt-0.5 text-sm text-ink-500">
            {asset.type.replaceAll("_", " ")} · {asset.location}
          </p>
        )}

        <dl className="mt-4 grid grid-cols-2 gap-y-2 text-sm">
          <dt className="text-ink-500">Asset Value</dt>
          <dd className="text-right font-medium text-ink-800">{asset ? formatCompactCurrency(asset.valuation, asset.currency) : "—"}</dd>
          <dt className="text-ink-500">Offering</dt>
          <dd className="text-right font-medium text-ink-800">{formatCompactCurrency(offering.targetRaise, offering.currency)}</dd>
          <dt className="text-ink-500">Unit Price</dt>
          <dd className="text-right font-medium text-ink-800">{formatCurrency(offering.unitPrice, offering.currency, { maximumFractionDigits: 4 })}</dd>
          <dt className="text-ink-500">Minimum</dt>
          <dd className="text-right font-medium text-ink-800">{formatCurrency(offering.minimumInvestment, offering.currency)}</dd>
        </dl>

        <div className="mt-4">
          <div className="flex items-center justify-between text-xs text-ink-500">
            <span>Raised</span>
            <span className="tabular-nums">{formatPercent(pctRaised)}</span>
          </div>
          <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-surface-muted">
            <div className="h-full rounded-full bg-brand-600" style={{ width: `${Math.min(pctRaised, 100)}%` }} />
          </div>
        </div>

        <Link href={`/investor/opportunities/${offering.id}`} className="mt-5">
          <Button className="w-full" variant="secondary">
            View Opportunity
          </Button>
        </Link>
      </CardContent>
    </Card>
  );
}
