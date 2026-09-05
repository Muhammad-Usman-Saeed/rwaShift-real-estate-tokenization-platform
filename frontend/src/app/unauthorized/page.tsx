import Link from "next/link";
import { ShieldCheck } from "@/components/ui/icons";
import { Button } from "@/components/ui/Button";

export default function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="max-w-sm text-center">
        <ShieldCheck className="mx-auto h-10 w-10 text-ink-300" />
        <h1 className="mt-4 text-lg font-semibold text-ink-900">Access restricted</h1>
        <p className="mt-2 text-sm text-ink-500">
          Your account role doesn&apos;t have access to this area of rwaShift. If you believe this is a mistake, contact
          your organization administrator.
        </p>
        <Link href="/" className="mt-6 inline-block">
          <Button variant="secondary">Return home</Button>
        </Link>
      </div>
    </div>
  );
}
