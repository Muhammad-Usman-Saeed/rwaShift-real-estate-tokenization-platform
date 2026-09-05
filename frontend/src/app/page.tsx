import { redirect } from "next/navigation";
import { auth } from "@/lib/auth/auth";
import { resolveDefaultPortal } from "@/lib/auth/roles";
import { PORTAL_HOME } from "@/lib/utils/constants";

export default async function RootPage() {
  const session = await auth();
  if (!session) {
    redirect("/login");
  }
  const portal = resolveDefaultPortal(session.roles);
  if (!portal) {
    redirect("/unauthorized");
  }
  redirect(PORTAL_HOME[portal]);
}
