import type { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

const TONE_CLASSES = {
  info: "border-info-500/30 bg-info-50 text-info-700",
  warning: "border-warning-500/30 bg-warning-50 text-warning-700",
  neutral: "border-surface-border bg-surface-subtle text-ink-600",
} as const;

/** Used for demo-scope labeling ("DEMO SETTLEMENT", "SIMULATED") so it's never mistaken for a live financial system. */
export function Callout({ tone = "neutral", children, className }: { tone?: keyof typeof TONE_CLASSES; children: ReactNode; className?: string }) {
  return (
    <div className={cn("rounded-md border px-3 py-2 text-xs font-medium", TONE_CLASSES[tone], className)}>{children}</div>
  );
}
