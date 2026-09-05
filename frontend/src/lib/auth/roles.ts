import { ADMIN_ROLES, INVESTOR_ROLES, ISSUER_ROLES, ROLES, type Role } from "@/lib/utils/constants";

export function hasRole(roles: string[], role: Role): boolean {
  return roles.includes(role);
}

export function hasAnyRole(roles: string[], allowed: readonly Role[]): boolean {
  return allowed.some((r) => roles.includes(r));
}

export function isPlatformAdmin(roles: string[]): boolean {
  return hasRole(roles, ROLES.PLATFORM_ADMIN);
}

export function canAccessIssuerPortal(roles: string[]): boolean {
  return hasAnyRole(roles, ISSUER_ROLES) || isPlatformAdmin(roles);
}

export function canAccessAdminPortal(roles: string[]): boolean {
  return hasAnyRole(roles, ADMIN_ROLES);
}

export function canAccessInvestorPortal(roles: string[]): boolean {
  return hasAnyRole(roles, INVESTOR_ROLES) || isPlatformAdmin(roles);
}

/** Where to land a freshly-authenticated user, in priority order (an admin lands in Admin first, not Issuer). */
export function resolveDefaultPortal(roles: string[]): "admin" | "issuer" | "investor" | null {
  if (canAccessAdminPortal(roles)) return "admin";
  if (canAccessIssuerPortal(roles)) return "issuer";
  if (canAccessInvestorPortal(roles)) return "investor";
  return null;
}
