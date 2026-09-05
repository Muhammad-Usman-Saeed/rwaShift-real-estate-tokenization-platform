import { ArrowDown } from "@/components/ui/icons";
import { cn } from "@/lib/utils/cn";

export interface FlowNode {
  label: string;
  sublabel?: string;
  emphasis?: boolean;
}

/**
 * Renders the Property → Legal Structure (SPV) → Offering → ERC-3643 Units chain that appears
 * throughout the product (Asset detail, Legal Structure, Opportunity detail). Deliberately never
 * implies the token itself owns the physical building — the SPV node is always the one carrying
 * "owns the property," and the token node is always labeled as investment units in the SPV, not
 * the property itself. See product spec section 6.
 */
export function OwnershipFlowDiagram({ nodes }: { nodes: FlowNode[] }) {
  return (
    <div className="flex flex-col items-center gap-1">
      {nodes.map((node, index) => (
        <div key={node.label} className="flex flex-col items-center">
          <div
            className={cn(
              "min-w-[14rem] rounded-lg border px-4 py-3 text-center shadow-card",
              node.emphasis ? "border-brand-300 bg-brand-50" : "border-surface-border bg-white",
            )}
          >
            <p className={cn("text-sm font-semibold", node.emphasis ? "text-brand-800" : "text-ink-800")}>{node.label}</p>
            {node.sublabel && <p className="mt-0.5 text-xs text-ink-500">{node.sublabel}</p>}
          </div>
          {index < nodes.length - 1 && <ArrowDown className="my-1 h-4 w-4 text-ink-300" />}
        </div>
      ))}
    </div>
  );
}
