import { NextResponse } from "next/server";
import { auth } from "@/lib/auth/auth";
import { canAccessAdminPortal, canAccessInvestorPortal, canAccessIssuerPortal } from "@/lib/auth/roles";

/**
 * UX-only route protection, exactly as the product spec requires: this only prevents a signed-in
 * user from *seeing* a portal their role doesn't cover, and bounces unauthenticated visitors to
 * login. It is not a security boundary — every API call is independently authorized by the
 * backend (see `shared.security.TenantContext`/`@PreAuthorize`), which remains authoritative.
 */
export default auth((req) => {
  const { pathname } = req.nextUrl;
  const session = req.auth;

  const isPublic =
    pathname === "/login" ||
    pathname === "/signup" ||
    pathname === "/unauthorized" ||
    pathname === "/" ||
    pathname.startsWith("/api/auth");
  if (isPublic) {
    return NextResponse.next();
  }

  if (!session) {
    const loginUrl = new URL("/login", req.url);
    loginUrl.searchParams.set("callbackUrl", pathname);
    return NextResponse.redirect(loginUrl);
  }

  const roles = session.roles ?? [];

  if (pathname.startsWith("/issuer") && !canAccessIssuerPortal(roles)) {
    return NextResponse.redirect(new URL("/unauthorized", req.url));
  }
  if (pathname.startsWith("/admin") && !canAccessAdminPortal(roles)) {
    return NextResponse.redirect(new URL("/unauthorized", req.url));
  }
  if (pathname.startsWith("/investor") && !canAccessInvestorPortal(roles)) {
    return NextResponse.redirect(new URL("/unauthorized", req.url));
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)"],
};
