"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";
import { activityApi } from "@/lib/api/activity";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken, useCurrentUser } from "@/lib/auth/session";
import { formatNumber } from "@/lib/utils/format";

function activityHrefForPathname(pathname: string): string {
  if (pathname.startsWith("/issuer")) return "/issuer/activity";
  if (pathname.startsWith("/investor")) return "/investor/activity";
  return "/admin/activity";
}

const RECORDED_VISIT_STORAGE_KEY = "rwashift.activity.lastVisitRecordedDate";

function todayKey(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Scrolling banner of platform-wide news (see `ActivityApplicationService` on the backend for
 * what counts as "newsworthy") plus visitor counts, shown above the sidebar/topbar on every
 * authenticated portal page (wired into `PortalShell`). Also fires the once-a-day "visit" beacon
 * that the visitor-count stats are built from — guarded by localStorage so it only actually hits
 * the network once per calendar day even though this component mounts on every page navigation
 * (the backend is idempotent per user/day too, this just avoids the redundant request).
 */
export function NewsTicker() {
  const token = useAccessToken();
  const { userId } = useCurrentUser();
  const pathname = usePathname();
  const recordedRef = useRef<string | null>(null);

  useEffect(() => {
    if (!token || !userId) return;
    if (typeof window === "undefined") return;
    // Keyed by user id, not just date — otherwise switching accounts in the same browser (e.g.
    // during testing) would suppress the beacon for the second user for the rest of the day.
    const storageKey = `${RECORDED_VISIT_STORAGE_KEY}.${userId}`;
    const today = todayKey();
    if (recordedRef.current === storageKey || window.localStorage.getItem(storageKey) === today) {
      recordedRef.current = storageKey;
      return;
    }
    recordedRef.current = storageKey;
    activityApi
      .recordVisit(token)
      .then(() => window.localStorage.setItem(storageKey, today))
      .catch(() => {
        recordedRef.current = null;
      });
  }, [token, userId]);

  // Milestone events (org/asset/offering lifecycle) are rare compared to routine per-investment
  // events (KYC steps, payments, token issuance) — a small window fills up with routine noise and
  // pushes milestones out entirely. 50 (the API's own default) isn't a permanent fix at high
  // volume, just enough headroom that a normal demo/test session doesn't lose them.
  const feed = useAuthedQuery(["activity", "ticker-feed"], (t) => activityApi.feed(50, t), { refetchInterval: 60_000 });
  const stats = useAuthedQuery(["activity", "ticker-stats"], (t) => activityApi.visitStats(t), { refetchInterval: 60_000 });

  const newsEntries = (feed.data ?? []).map((item) => item.message);
  const statsEntry = stats.data
    ? `${formatNumber(stats.data.visitorsToday)} visitors today · ${formatNumber(stats.data.visitorsThisWeek)} this week · ${formatNumber(stats.data.visitorsThisMonth)} this month · ${formatNumber(stats.data.visitorsThisYear)} this year`
    : null;

  const entries = [...(statsEntry ? [statsEntry] : []), ...newsEntries];

  if (entries.length === 0) {
    return null;
  }

  // Duplicated so the marquee animation's -50% end state seams back into an identical start.
  const track = [...entries, ...entries];

  return (
    <Link
      href={activityHrefForPathname(pathname ?? "")}
      className="group flex h-8 shrink-0 items-center overflow-hidden border-b border-brand-800 bg-brand-900 text-white"
      aria-label="View all platform activity"
    >
      <div className="flex shrink-0 animate-marquee items-center whitespace-nowrap group-hover:[animation-play-state:paused]">
        {track.map((entry, index) => (
          <span key={index} className="mx-6 text-xs font-medium tracking-wide">
            {entry}
          </span>
        ))}
      </div>
    </Link>
  );
}
