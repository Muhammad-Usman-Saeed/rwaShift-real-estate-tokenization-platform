"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { NEXT_PUBLIC_CHAIN_LABEL, NEXT_PUBLIC_CHAIN_ID } from "@/lib/utils/env";

export default function SystemPage() {
  return (
    <div className="max-w-2xl">
      <PageHeader title="System" description="Platform environment configuration." />

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Blockchain Network</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <Term label="Network" value={NEXT_PUBLIC_CHAIN_LABEL} />
              <Term label="Chain ID" value={NEXT_PUBLIC_CHAIN_ID} />
              <Term label="Token Standard" value="ERC-3643" />
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>API</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-1 gap-4 text-sm">
              <Term label="API Base URL" value={process.env.NEXT_PUBLIC_API_BASE_URL} mono />
            </dl>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Term({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-500">{label}</dt>
      <dd className={`mt-1 font-medium text-ink-900 ${mono ? "font-mono text-xs" : ""}`}>{value}</dd>
    </div>
  );
}
