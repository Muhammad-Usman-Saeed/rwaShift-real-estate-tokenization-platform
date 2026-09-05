"use client";

import { type ReactNode, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { networkApi } from "@/lib/api/network";
import { NetworkStatusPanel } from "@/components/domain/NetworkStatusPanel";
import { ShieldCheck, Building, Coins, ChevronDown } from "@/components/ui/icons";
import { cn } from "@/lib/utils/cn";

const FEATURES = [
  { icon: ShieldCheck, text: "ERC-3643 compliant, on-chain investor eligibility" },
  { icon: Building, text: "Institutional-grade real estate asset structuring" },
  { icon: Coins, text: "Transparent, auditable capital and distribution flows" },
];

/**
 * Shared split-panel shell for /login and /signup. The brand panel carries the platform's actual
 * positioning (a login screen's first impression), with live network status demoted to a
 * collapsed-by-default badge — full RPC/chain-activity detail is genuinely useful for a technical
 * demo, but showing raw block times and tx hashes before a "sign in" button reads as an
 * engineering console, not an institutional platform. Expanding it is one click away for anyone
 * who wants it.
 */
export function AuthShell({ title, subtitle, children, footer }: { title: string; subtitle: string; children: ReactNode; footer?: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-ink-950">
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden bg-ink-900 px-12 py-12 lg:flex">
        <div
          className="pointer-events-none absolute inset-0 opacity-40"
          style={{
            backgroundImage:
              "radial-gradient(circle at 20% 15%, rgba(163,180,222,0.10), transparent 45%), radial-gradient(circle at 80% 85%, rgba(191,157,72,0.10), transparent 45%)",
          }}
        />
        <div className="relative">
          <img src="/logo.png" alt="rwaShift" className="h-14 w-auto object-contain object-left" />

          <h1 className="mt-16 max-w-sm text-3xl font-semibold leading-tight text-white">
            Real estate investment, structured on-chain.
          </h1>
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-ink-300">
            Institutional infrastructure for issuing, managing, and settling tokenized real estate offerings — not a
            public trading platform.
          </p>

          <ul className="mt-10 flex flex-col gap-4">
            {FEATURES.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3">
                <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-ink-800 text-gold-300">
                  <Icon className="h-4 w-4" />
                </div>
                <p className="mt-0.5 text-sm text-ink-200">{text}</p>
              </li>
            ))}
          </ul>
        </div>

        <div className="relative">
          <NetworkStatusBadge />
        </div>
      </div>

      <div className="flex flex-1 flex-col items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <img src="/logo-mark.png" alt="" className="h-8 w-8 shrink-0 object-contain" />
            <p className="text-sm font-semibold text-white">rwaShift</p>
          </div>

          <div className="mb-7">
            <h2 className="text-2xl font-semibold text-white">{title}</h2>
            <p className="mt-2 text-sm text-ink-300">{subtitle}</p>
          </div>

          <div className="rounded-lg border border-ink-800 bg-ink-900 p-8 shadow-raised">{children}</div>

          {footer && <div className="mt-4 text-center text-xs text-ink-400">{footer}</div>}
        </div>
      </div>
    </div>
  );
}

function NetworkStatusBadge() {
  const [expanded, setExpanded] = useState(false);
  const { data, isError } = useQuery({
    queryKey: ["network-status"],
    queryFn: networkApi.status,
    refetchInterval: 20_000,
    retry: 1,
  });

  const isReachable = !isError && data?.reachable;

  if (expanded) {
    return (
      <div>
        <button
          type="button"
          onClick={() => setExpanded(false)}
          className="mb-3 flex items-center gap-1.5 text-xs font-medium text-ink-400 hover:text-ink-200"
        >
          <ChevronDown className="h-3.5 w-3.5 rotate-180" />
          Hide network details
        </button>
        <NetworkStatusPanel />
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={() => setExpanded(true)}
      aria-label={isReachable ? `Connected to ${data?.network ?? "network"} — view network details` : "Network unavailable — view network details"}
      className="flex w-full items-center justify-between gap-3 rounded-md border border-ink-800 bg-ink-950 px-4 py-3 text-left hover:border-ink-700"
    >
      <div className="flex items-center gap-2.5">
        <span
          className={cn("h-2 w-2 shrink-0 rounded-full", isReachable ? "bg-success-500" : "bg-danger-500")}
          aria-hidden="true"
        />
        <span className="text-xs text-ink-300">
          {isReachable ? `Connected to ${data?.network ?? "network"}` : "Network unavailable"}
        </span>
      </div>
      <ChevronDown className="h-3.5 w-3.5 shrink-0 text-ink-500" />
    </button>
  );
}
