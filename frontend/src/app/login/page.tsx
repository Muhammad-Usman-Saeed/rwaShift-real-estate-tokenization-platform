import Link from "next/link";
import { AuthShell } from "@/components/domain/AuthShell";
import { SignInButton } from "@/components/domain/SignInButton";
import { WalletSignInButton } from "@/components/domain/WalletSignInButton";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ callbackUrl?: string; created?: string }>;
}) {
  const { callbackUrl, created } = await searchParams;

  return (
    <AuthShell title="Sign in to your account" subtitle="Platform admins, issuers, and investors all sign in here.">
      {created && (
        <div className="mb-4 rounded-md border border-success-500/30 bg-success-50 p-3 text-center text-sm text-success-700">
          Account created — sign in below.
        </div>
      )}
      <SignInButton callbackUrl={callbackUrl} />
      <p className="mt-3 text-center text-xs text-ink-400">You&apos;ll be redirected to rwaShift&apos;s secure sign-in.</p>

      <div className="mt-7 rounded-md border border-ink-800 bg-ink-950/60 p-4">
        <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-ink-500">For Investors</p>
        <WalletSignInButton callbackUrl={callbackUrl} />
        <p className="mt-2 text-center text-xs text-ink-400">Sign a message to prove wallet ownership — no gas, no transaction.</p>
        <p className="mt-3 text-center text-xs text-ink-400">
          New investor?{" "}
          <Link href="/signup" className="text-gold-300 hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}
