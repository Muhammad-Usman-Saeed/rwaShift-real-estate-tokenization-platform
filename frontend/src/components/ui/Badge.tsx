import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

const badgeVariants = cva("inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium", {
  variants: {
    tone: {
      neutral: "bg-surface-muted text-ink-700",
      brand: "bg-brand-50 text-brand-700",
      gold: "bg-gold-50 text-gold-700",
      success: "bg-success-50 text-success-700",
      warning: "bg-warning-50 text-warning-700",
      danger: "bg-danger-50 text-danger-700",
      info: "bg-info-50 text-info-600",
    },
  },
  defaultVariants: { tone: "neutral" },
});

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, tone, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ tone }), className)} {...props} />;
}

const DOT_TONE_CLASSES: Record<string, string> = {
  neutral: "bg-ink-400",
  brand: "bg-brand-600",
  gold: "bg-gold-500",
  success: "bg-success-500",
  warning: "bg-warning-500",
  danger: "bg-danger-500",
  info: "bg-info-500",
};

export function BadgeDot({ tone = "neutral", className }: { tone?: keyof typeof DOT_TONE_CLASSES; className?: string }) {
  return <span className={cn("h-1.5 w-1.5 shrink-0 rounded-full", DOT_TONE_CLASSES[tone], className)} aria-hidden="true" />;
}
