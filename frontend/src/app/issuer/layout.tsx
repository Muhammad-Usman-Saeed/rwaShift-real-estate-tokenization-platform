import type { ReactNode } from "react";
import { PortalShell } from "@/components/layout/PortalShell";
import { issuerNav } from "@/components/layout/nav-config";

export default function IssuerLayout({ children }: { children: ReactNode }) {
  return (
    <PortalShell portalLabel="Issuer Portal" navItems={issuerNav} title="Issuer Portal">
      {children}
    </PortalShell>
  );
}
