import type { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

export function StatTile({
  label,
  value,
  icon,
  trend,
  className,
}: {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
  trend?: { value: string; direction: "up" | "down" | "flat" };
  className?: string;
}) {
  return (
    <div className={cn("rounded-lg border border-surface-border bg-white p-5 shadow-card", className)}>
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium uppercase tracking-wide text-ink-500">{label}</p>
        {icon && <div className="text-ink-300">{icon}</div>}
      </div>
      <p className="mt-2 tabular-nums text-2xl font-semibold text-ink-900">{value}</p>
      {trend && (
        <p
          className={cn(
            "mt-1 text-xs font-medium",
            trend.direction === "up" && "text-success-600",
            trend.direction === "down" && "text-danger-600",
            trend.direction === "flat" && "text-ink-500",
          )}
        >
          {trend.value}
        </p>
      )}
    </div>
  );
}
