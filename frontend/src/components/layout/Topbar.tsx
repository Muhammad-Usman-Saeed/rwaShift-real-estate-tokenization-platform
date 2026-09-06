"use client";

import * as Dialog from "@radix-ui/react-dialog";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { rpInitiatedSignOut } from "@/lib/auth/actions";
import { NotificationBell } from "@/components/layout/NotificationBell";
import { NewsButton } from "@/components/layout/NewsButton";
import { useCurrentUser } from "@/lib/auth/session";
import { canAccessAdminPortal, canAccessIssuerPortal, canAccessInvestorPortal } from "@/lib/auth/roles";
import { Button } from "@/components/ui/Button";
import { LogOut, X } from "@/components/ui/icons";
import { cn } from "@/lib/utils/cn";
import type { NavItem } from "@/components/layout/nav-config";

const PORTAL_SWITCH_LINKS = [
  { key: "admin", label: "Platform Admin", href: "/admin/dashboard", canAccess: canAccessAdminPortal },
  { key: "issuer", label: "Issuer Portal", href: "/issuer/dashboard", canAccess: canAccessIssuerPortal },
  { key: "investor", label: "Investor Portal", href: "/investor/opportunities", canAccess: canAccessInvestorPortal },
] as const;

export function Topbar({ title, mobileNavItems }: { title: string; mobileNavItems: NavItem[] }) {
  const { roles, userId } = useCurrentUser();
  const [mobileOpen, setMobileOpen] = useState(false);
  const availablePortals = PORTAL_SWITCH_LINKS.filter((p) => p.canAccess(roles));

  return (
    <header className="flex h-16 items-center justify-between border-b border-surface-border bg-white px-4 lg:px-6">
      <div className="flex items-center gap-3">
        <Dialog.Root open={mobileOpen} onOpenChange={setMobileOpen}>
          <Dialog.Trigger asChild>
            <button
              className="rounded-md p-2 text-ink-600 hover:bg-surface-muted lg:hidden"
              aria-label="Open navigation menu"
            >
              <MenuIcon />
            </button>
          </Dialog.Trigger>
          <Dialog.Portal>
            <Dialog.Overlay className="fixed inset-0 z-40 bg-ink-950/40 lg:hidden" />
            <Dialog.Content className="fixed inset-y-0 left-0 z-50 w-64 bg-ink-950 p-4 lg:hidden">
              <div className="mb-4 flex items-center justify-between">
                <Dialog.Title className="flex items-center gap-2 text-sm font-semibold text-white">
                  <img src="/logo-rs-sharp.png" alt="" className="h-6 w-6 shrink-0 object-contain" />
                  rwa<span className="text-gold-400">S</span>hift
                </Dialog.Title>
                <Dialog.Close aria-label="Close menu" className="text-ink-400">
                  <X className="h-5 w-5" />
                </Dialog.Close>
              </div>
              <nav className="space-y-0.5">
                {mobileNavItems.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setMobileOpen(false)}
                    className="flex items-center gap-3 px-3 py-2 text-sm font-medium text-ink-200 hover:bg-ink-900 hover:text-white"
                  >
                    {item.icon}
                    {item.label}
                  </Link>
                ))}
              </nav>
            </Dialog.Content>
          </Dialog.Portal>
        </Dialog.Root>
        <h2 className="text-sm font-semibold text-ink-800">{title}</h2>
      </div>

      <div className="flex items-center gap-3">
        {availablePortals.length > 1 && (
          <div className="hidden items-center gap-1 border border-surface-border bg-surface-subtle p-1 sm:flex">
            {availablePortals.map((portal) => (
              <PortalSwitchLink key={portal.key} href={portal.href} label={portal.label} />
            ))}
          </div>
        )}
        <NewsButton />
        <NotificationBell />
        <div className="hidden text-right sm:block">
          <p className="text-xs font-medium text-ink-700">{userId}</p>
          <p className="text-[10px] uppercase tracking-wide text-ink-400">{roles.join(", ")}</p>
        </div>
        <form action={rpInitiatedSignOut}>
          <Button variant="ghost" size="icon" aria-label="Sign out" type="submit">
            <LogOut className="h-4 w-4" />
          </Button>
        </form>
      </div>
    </header>
  );
}

function PortalSwitchLink({ href, label }: { href: string; label: string }) {
  const pathname = usePathname();
  const isActive = pathname.startsWith(href.split("/").slice(0, 2).join("/"));
  return (
    <Link
      href={href}
      className={cn(
        "rounded px-2.5 py-1 text-xs font-medium transition-colors",
        isActive ? "bg-white text-ink-900 shadow-sm" : "text-ink-500 hover:text-ink-800",
      )}
    >
      {label}
    </Link>
  );
}

function MenuIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="h-5 w-5" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  );
}
