import type { ReactNode } from "react";
import { PortalShell } from "@/components/layout/PortalShell";
import { adminNav } from "@/components/layout/nav-config";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <PortalShell portalLabel="Platform Admin" navItems={adminNav} title="Platform Admin">
      {children}
    </PortalShell>
  );
}
