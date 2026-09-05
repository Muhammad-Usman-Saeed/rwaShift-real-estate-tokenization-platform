import type { ReactNode } from "react";
import { PortalShell } from "@/components/layout/PortalShell";
import { investorNav } from "@/components/layout/nav-config";

export default function InvestorLayout({ children }: { children: ReactNode }) {
  return (
    <PortalShell portalLabel="Investor Portal" navItems={investorNav} title="Investor Portal">
      {children}
    </PortalShell>
  );
}
