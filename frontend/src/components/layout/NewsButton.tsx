"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { activityApi } from "@/lib/api/activity";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { queryKeys } from "@/lib/api/query-keys";
import { formatRelativeTime } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";
import { Activity as ActivityIcon } from "@/components/ui/icons";

const SEEN_IDS_STORAGE_KEY = "rwashift.activity.seenNewsIds";

function activityHrefForPathname(pathname: string): string {
  if (pathname.startsWith("/issuer")) return "/issuer/activity";
  if (pathname.startsWith("/investor")) return "/investor/activity";
  return "/admin/activity";
}

function loadSeenIds(): Set<string> {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = window.localStorage.getItem(SEEN_IDS_STORAGE_KEY);
    return raw ? new Set(JSON.parse(raw) as string[]) : new Set();
  } catch {
    return new Set();
  }
}

function saveSeenIds(ids: Set<string>) {
  window.localStorage.setItem(SEEN_IDS_STORAGE_KEY, JSON.stringify(Array.from(ids)));
}

/**
 * Same feed the top-of-screen `NewsTicker` scrolls through, but reachable instantly from the
 * topbar without waiting for the item you want to scroll past — a quick-access panel rather than
 * a second data source. "Seen" is tracked the same way as `NotificationBell`: client-side by
 * stable item id, since the backend feed itself has no per-user read state (see
 * `ActivityApplicationService`'s Javadoc).
 */
export function NewsButton() {
  const [open, setOpen] = useState(false);
  const [seenIds, setSeenIds] = useState<Set<string>>(() => new Set());
  const containerRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<Map<string, HTMLElement>>(new Map());
  const pathname = usePathname();

  useEffect(() => {
    setSeenIds(loadSeenIds());
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Same query key as ActivityFeedView's own feed query (queryKeys.activity.feed()) — must use the
  // same limit, since React Query caches by key alone, not by the params passed to the query
  // function; a mismatched limit here would have the two components fighting over one cache entry.
  const { data: feed } = useAuthedQuery(queryKeys.activity.feed(), (t) => activityApi.feed(50, t), {
    refetchInterval: 30_000,
  });

  const items = feed ?? [];
  const unreadCount = items.filter((item) => !seenIds.has(item.id)).length;

  const markSeen = useCallback((ids: string[]) => {
    if (ids.length === 0) return;
    setSeenIds((prev) => {
      if (ids.every((id) => prev.has(id))) return prev;
      const next = new Set(prev);
      ids.forEach((id) => next.add(id));
      saveSeenIds(next);
      return next;
    });
  }, []);

  function markAllSeen() {
    markSeen(items.map((item) => item.id));
  }

  function handleOpen() {
    setOpen((v) => !v);
  }

  // As the user scrolls an item into view inside the dropdown, mark it seen — the unread badge
  // ticks down live instead of only clearing all-at-once via the explicit button. Plain
  // getBoundingClientRect math on scroll rather than IntersectionObserver: this list is capped at
  // 20 items, so there's no performance need for the more complex API, and a scroll-event handler
  // doesn't depend on the browser having actually painted a frame to fire (IntersectionObserver
  // callbacks are tied to the rendering pipeline and can lag behind a programmatic scroll).
  const checkVisibleItems = useCallback(() => {
    const container = scrollRef.current;
    if (!container) return;
    const containerRect = container.getBoundingClientRect();
    const visibleIds: string[] = [];
    itemRefs.current.forEach((el, id) => {
      const rect = el.getBoundingClientRect();
      const visibleHeight = Math.min(rect.bottom, containerRect.bottom) - Math.max(rect.top, containerRect.top);
      if (visibleHeight >= rect.height * 0.6) {
        visibleIds.push(id);
      }
    });
    markSeen(visibleIds);
  }, [markSeen]);

  useEffect(() => {
    if (!open) return;
    // Run once on open (for whatever's visible without scrolling) and then on every scroll tick.
    checkVisibleItems();
  }, [open, items, checkVisibleItems]);

  const registerItemRef = useCallback((id: string, el: HTMLElement | null) => {
    if (el) itemRefs.current.set(id, el);
    else itemRefs.current.delete(id);
  }, []);

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={handleOpen}
        aria-label={unreadCount > 0 ? `News, ${unreadCount} new` : "News"}
        className="relative rounded-md p-2 text-ink-600 hover:bg-surface-muted"
      >
        <ActivityIcon className="h-4 w-4" />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand-600 px-1 text-[10px] font-semibold leading-none text-white">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-md border border-surface-border bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-surface-border px-3 py-2">
            <p className="text-sm font-semibold text-ink-800">News</p>
            {unreadCount > 0 && (
              <button type="button" onClick={markAllSeen} className="text-xs font-medium text-ink-500 hover:text-ink-800">
                Mark all as read
              </button>
            )}
          </div>
          <div ref={scrollRef} onScroll={checkVisibleItems} className="max-h-96 overflow-y-auto">
            {items.length === 0 ? (
              <p className="px-3 py-6 text-center text-sm text-ink-400">No news yet.</p>
            ) : (
              items.map((item) => {
                const isUnread = !seenIds.has(item.id);
                return (
                  <div
                    key={item.id}
                    data-item-id={item.id}
                    ref={(el) => registerItemRef(item.id, el)}
                    className={cn("flex items-start gap-2 border-b border-surface-border px-3 py-2.5 last:border-b-0", isUnread && "bg-brand-50/60")}
                  >
                    {isUnread && <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-600" />}
                    <div className={cn("min-w-0 flex-1", !isUnread && "pl-3.5")}>
                      <p className="text-sm text-ink-800">{item.message}</p>
                      <p className="mt-1 text-[10px] uppercase tracking-wide text-ink-400">
                        {item.resourceType} · {formatRelativeTime(item.occurredAt)}
                      </p>
                    </div>
                  </div>
                );
              })
            )}
          </div>
          <Link
            href={activityHrefForPathname(pathname ?? "")}
            onClick={() => {
              markAllSeen();
              setOpen(false);
            }}
            className="block border-t border-surface-border px-3 py-2 text-center text-xs font-medium text-brand-700 hover:bg-surface-subtle"
          >
            View all activity
          </Link>
        </div>
      )}
    </div>
  );
}
