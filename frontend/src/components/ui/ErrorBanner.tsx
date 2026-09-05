"use client";

import { toFriendlyError } from "@/lib/errors/problem-details";
import { rpInitiatedSignOut } from "@/lib/auth/actions";
import { AlertTriangle } from "@/components/ui/icons";
import { cn } from "@/lib/utils/cn";

export function ErrorBanner({ error, className }: { error: unknown; className?: string }) {
  const friendly = toFriendlyError(error);
  return (
    <div
      role="alert"
      className={cn("flex items-start gap-3 rounded-md border border-danger-500/30 bg-danger-50 p-4", className)}
    >
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-danger-600" />
      <div className="flex-1">
        <p className="text-sm font-semibold text-danger-700">{friendly.title}</p>
        <p className="mt-0.5 text-sm text-danger-700/90">{friendly.message}</p>
        {friendly.fieldErrors && (
          <ul className="mt-2 list-disc space-y-0.5 pl-5 text-xs text-danger-700/90">
            {Object.entries(friendly.fieldErrors).map(([field, message]) => (
              <li key={field}>
                <span className="font-medium">{field}</span>: {message}
              </li>
            ))}
          </ul>
        )}
        {friendly.correlationId && (
          <p className="mt-2 text-xs text-danger-700/60">Reference: {friendly.correlationId}</p>
        )}
        {/*
          A 401 that reaches this far means the cached session token was stale when the request
          fired — SessionWatcher's automatic recovery only triggers when NextAuth's own proactive
          refresh fails, which an idle tab holding an already-stale token can miss entirely. This
          is the fallback affordance: previously this banner was a dead end with nothing to click.
        */}
        {friendly.kind === "unauthorized" && (
          <button
            type="button"
            onClick={() => void rpInitiatedSignOut()}
            className="mt-3 text-sm font-medium text-danger-700 underline underline-offset-2 hover:text-danger-800"
          >
            Sign in again
          </button>
        )}
      </div>
    </div>
  );
}
