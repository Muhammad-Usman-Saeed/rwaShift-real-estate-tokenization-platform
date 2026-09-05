"use client";

import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { offeringsApi } from "@/lib/api/offerings";
import { distributionsApi } from "@/lib/api/distributions";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCompactCurrency, formatDate } from "@/lib/utils/format";

const BRAND = "#1d3f93";
const GOLD = "#c08d13";

export default function IssuerReportsPage() {
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));
  const distributions = useAuthedQuery(queryKeys.distributions.list(), (token) => distributionsApi.list(token));

  const offeringProgressData = (offerings.data ?? []).map((o) => ({
    name: o.name.length > 18 ? `${o.name.slice(0, 18)}…` : o.name,
    Issued: o.unitsIssued,
    Available: o.unitsAvailable,
  }));

  const capitalRaisedData = (offerings.data ?? []).map((o) => ({
    name: o.name.length > 18 ? `${o.name.slice(0, 18)}…` : o.name,
    Raised: o.unitPrice * o.unitsIssued,
    Target: o.targetRaise,
  }));

  const distributionHistoryData = (distributions.data ?? [])
    .slice()
    .sort((a, b) => new Date(a.recordDate).getTime() - new Date(b.recordDate).getTime())
    .map((d) => ({ name: formatDate(d.recordDate, { month: "short", day: "numeric", year: undefined }), Amount: d.totalAmount }));

  return (
    <div>
      <PageHeader title="Reports" description="Portfolio-level performance across your organization's offerings." />

      {(offerings.isLoading || distributions.isLoading) && <SkeletonText lines={6} />}
      {offerings.isError && <ErrorBanner error={offerings.error} />}

      {offerings.data && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Offering Progress</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={offeringProgressData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ea" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="Issued" stackId="a" fill={BRAND} radius={[0, 0, 0, 0]} />
                    <Bar dataKey="Available" stackId="a" fill="#c9d3e0" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Capital Raised vs. Target</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={capitalRaisedData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ea" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => formatCompactCurrency(v)} />
                    <Tooltip formatter={(v: number) => formatCompactCurrency(v)} />
                    <Bar dataKey="Raised" fill={GOLD} radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Target" fill="#eef0f3" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Distribution History</CardTitle>
            </CardHeader>
            <CardContent>
              {distributionHistoryData.length === 0 ? (
                <p className="py-8 text-center text-sm text-ink-500">No distributions recorded yet.</p>
              ) : (
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={distributionHistoryData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e1e4ea" vertical={false} />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => formatCompactCurrency(v)} />
                      <Tooltip formatter={(v: number) => formatCompactCurrency(v)} />
                      <Bar dataKey="Amount" fill={BRAND} radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
