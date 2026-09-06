"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils/cn";
import { PORTAL_HOME } from "@/lib/utils/constants";
import type { NavItem } from "@/components/layout/nav-config";

export function Sidebar({ items, portalLabel }: { items: NavItem[]; portalLabel: string }) {
  const pathname = usePathname();
  const portalKey = (pathname.split("/")[1] ?? "") as keyof typeof PORTAL_HOME;
  const homeHref = PORTAL_HOME[portalKey] ?? "/";

  return (
    <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-ink-800 bg-ink-950 lg:flex">
      <Link href={homeHref} className="flex h-16 items-center gap-2 border-b border-ink-800 px-5 hover:bg-ink-900">
        <img src="/logo-rs-sharp.png" alt="" className="h-8 w-8 shrink-0 object-contain" />
        <div className="leading-tight">
          <p className="text-sm font-semibold text-white">
            rwa<span className="text-gold-400">S</span>hift
          </p>
          <p className="text-[10px] uppercase tracking-wide text-ink-400">{portalLabel}</p>
        </div>
      </Link>
      <nav aria-label={`${portalLabel} navigation`} className="flex-1 space-y-0.5 overflow-y-auto px-3 py-4">
        {items.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "flex items-center gap-3 px-3 py-2 text-sm font-medium transition-colors",
                isActive ? "bg-ink-800 text-white" : "text-ink-300 hover:bg-ink-900 hover:text-white",
              )}
            >
              {item.icon}
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
