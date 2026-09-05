"use client";

import { useState } from "react";
import { Copy, Check } from "@/components/ui/icons";
import { cn } from "@/lib/utils/cn";
import { useToast } from "@/components/ui/Toast";

interface CopyButtonProps {
  value: string;
  label?: string;
  className?: string;
}

export function CopyButton({ value, label = "Copy to clipboard", className }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);
  const { push } = useToast();

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      push({ title: "Copied to clipboard", variant: "success" });
      setTimeout(() => setCopied(false), 1500);
    } catch {
      push({ title: "Copy failed", variant: "error" });
    }
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      aria-label={label}
      title={label}
      className={cn("inline-flex h-6 w-6 shrink-0 items-center justify-center rounded text-ink-400 hover:bg-surface-muted hover:text-ink-700", className)}
    >
      {copied ? <Check className="h-3.5 w-3.5 text-success-600" /> : <Copy className="h-3.5 w-3.5" />}
    </button>
  );
}
