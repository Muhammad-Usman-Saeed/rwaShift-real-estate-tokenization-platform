"use client";

import type { ReactNode } from "react";
import { useCurrentUser } from "@/lib/auth/session";
import { hasAnyRole } from "@/lib/auth/roles";
import type { Role } from "@/lib/utils/constants";

/**
 * Action-level UI gating (e.g. hiding an Approve button from a role that can't perform it). This
 * is UX only — the backend's own `@PreAuthorize` checks are what actually enforce it, so hiding a
 * control here never substitutes for a server-side check.
 */
export function RoleGate({ allow, children, fallback = null }: { allow: readonly Role[]; children: ReactNode; fallback?: ReactNode }) {
  const { roles } = useCurrentUser();
  return hasAnyRole(roles, allow) ? <>{children}</> : <>{fallback}</>;
}
