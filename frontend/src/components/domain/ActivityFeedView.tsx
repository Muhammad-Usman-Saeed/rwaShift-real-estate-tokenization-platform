"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Badge, BadgeDot } from "@/components/ui/Badge";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Activity as ActivityIcon } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { activityApi, type ActivityFeedItem } from "@/lib/api/activity";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDateTime, formatNumber } from "@/lib/utils/format";

type Tone = "neutral" | "brand" | "gold" | "success" | "warning" | "danger" | "info";

/** Consistent color per event category — used both in the category legend and as a per-row dot in the feed, so the two stay visually linked. */
const CATEGORY_TONE: Record<string, Tone> = {
  Organization: "brand",
  Asset: "gold",
  LegalStructure: "info",
  Offering: "success",
  Investor: "brand",
  KycCase: "warning",
  Investment: "success",
  Distribution: "gold",
  Document: "neutral",
};

function categoryTone(category: string): Tone {
  return CATEGORY_TONE[category] ?? "neutral";
}

function dayLabel(occurredAt: string): string {
  const date = new Date(occurredAt);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  const isSameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString();
  if (isSameDay(date, today)) return "Today";
  if (isSameDay(date, yesterday)) return "Yesterday";
  return date.toLocaleDateString(undefined, { month: "long", day: "numeric", year: "numeric" });
}

function groupByDay(items: ActivityFeedItem[]): { day: string; items: ActivityFeedItem[] }[] {
  const groups: { day: string; items: ActivityFeedItem[] }[] = [];
  for (const item of items) {
    const day = dayLabel(item.occurredAt);
    const lastGroup = groups[groups.length - 1];
    if (lastGroup && lastGroup.day === day) {
      lastGroup.items.push(item);
    } else {
      groups.push({ day, items: [item] });
    }
  }
  return groups;
}

/**
 * Shared by every portal's /activity route (see nav-config.tsx). Deliberately compact at the
 * top — a wall of stat tiles was pushing the actual news out of view, which defeats the point of
 * a page meant to be skimmed. Visitor/investor counts collapse into one small row, the category
 * breakdown is an inline legend rather than its own tile grid, and the feed itself is grouped by
 * day with a color-coded dot per category so scanning doesn't mean reading 50 undifferentiated rows.
 */
export function ActivityFeedView() {
  const feed = useAuthedQuery(queryKeys.activity.feed(), (t) => activityApi.feed(50, t), { refetchInterval: 30_000 });
  const stats = useAuthedQuery(queryKeys.activity.visitStats(), (t) => activityApi.visitStats(t), { refetchInterval: 60_000 });
  const txStats = useAuthedQuery(queryKeys.activity.stats(), (t) => activityApi.stats(t), { refetchInterval: 30_000 });

  const categoryEntries = txStats.data ? Object.entries(txStats.data.countByCategory).sort((a, b) => b[1] - a[1]) : [];
  const groupedFeed = feed.data ? groupByDay(feed.data) : [];

  return (
    <div>
      <PageHeader title="Activity" description="Platform-wide news: what's happening across organizations, assets, and investors." />

      {(stats.isLoading || txStats.isLoading) && <SkeletonText lines={2} />}
      {stats.isError && <ErrorBanner error={stats.error} className="mb-4" />}
      {txStats.isError && <ErrorBanner error={txStats.error} className="mb-4" />}

      {stats.data && txStats.data && (
        <div className="mb-6 rounded-lg border border-surface-border bg-white p-4">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <CompactStat label="Today" visitors={stats.data.visitorsToday} investors={stats.data.investorsToday} />
            <CompactStat label="This Week" visitors={stats.data.visitorsThisWeek} investors={stats.data.investorsThisWeek} />
            <CompactStat label="This Month" visitors={stats.data.visitorsThisMonth} investors={stats.data.investorsThisMonth} />
            <CompactStat label="This Year" visitors={stats.data.visitorsThisYear} investors={stats.data.investorsThisYear} />
          </div>
          <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-surface-border pt-4">
            <span className="text-xs font-semibold uppercase tracking-wide text-ink-500">
              {formatNumber(txStats.data.totalCount)} Transactions
            </span>
            {categoryEntries.map(([category, count]) => (
              <Badge key={category} tone={categoryTone(category)}>
                <BadgeDot tone={categoryTone(category)} />
                {category} · {formatNumber(count)}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {feed.isLoading && <SkeletonText lines={8} />}
      {feed.isError && <ErrorBanner error={feed.error} />}
      {feed.data && feed.data.length === 0 && (
        <EmptyState icon={<ActivityIcon className="h-8 w-8" />} title="No activity yet" description="Platform news will appear here as things happen." />
      )}

      {groupedFeed.length > 0 && (
        <div className="flex flex-col gap-5">
          {groupedFeed.map((group) => (
            <div key={group.day}>
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-ink-400">{group.day}</p>
              <ul className="divide-y divide-surface-border rounded-md border border-surface-border bg-white">
                {group.items.map((item) => (
                  <li key={item.id} className="flex items-start gap-3 px-4 py-2.5">
                    <BadgeDot tone={categoryTone(item.resourceType)} className="mt-1.5" />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm text-ink-800">{item.message}</p>
                    </div>
                    <span className="shrink-0 text-xs text-ink-400">{formatDateTime(item.occurredAt)}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function CompactStat({ label, visitors, investors }: { label: string; visitors: number; investors: number }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-ink-400">{label}</p>
      <p className="mt-1 text-sm font-semibold text-ink-900">{formatNumber(visitors)} visitors</p>
      <p className="text-xs text-ink-500">{formatNumber(investors)} investors</p>
    </div>
  );
}
