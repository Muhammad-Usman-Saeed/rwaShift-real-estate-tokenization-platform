import type { ReactNode } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Topbar } from "@/components/layout/Topbar";
import { NewsTicker } from "@/components/layout/NewsTicker";
import type { NavItem } from "@/components/layout/nav-config";

export function PortalShell({
  portalLabel,
  navItems,
  title,
  children,
}: {
  portalLabel: string;
  navItems: NavItem[];
  title: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-surface-subtle">
      <NewsTicker />
      <div className="flex min-h-0 flex-1">
        <Sidebar items={navItems} portalLabel={portalLabel} />
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar title={title} mobileNavItems={navItems} />
          <main className="flex-1 px-4 py-6 lg:px-8 lg:py-8">
            <div className="mx-auto max-w-7xl">{children}</div>
          </main>
        </div>
      </div>
    </div>
  );
}
