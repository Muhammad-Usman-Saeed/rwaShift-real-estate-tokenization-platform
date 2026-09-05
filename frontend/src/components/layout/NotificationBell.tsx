"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { notificationsApi } from "@/lib/api/notifications";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { formatRelativeTime } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";
import { Bell } from "@/components/ui/icons";

const SEEN_IDS_STORAGE_KEY = "rwashift.notifications.seenIds";

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
 * There's no persisted notification/read-state table on the backend — `GET /notifications`
 * derives the list fresh from each unit's current state every call (see
 * `NotificationApplicationService`'s Javadoc). "Read" is therefore tracked here, client-side, by
 * stable item id in localStorage — good enough for a single-browser demo; a multi-device read
 * state would need the backend to persist it instead.
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [seenIds, setSeenIds] = useState<Set<string>>(() => new Set());
  const containerRef = useRef<HTMLDivElement>(null);

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

  const { data: notifications } = useAuthedQuery(
    ["notifications"],
    (token) => notificationsApi.list(token),
    { refetchInterval: 30_000 },
  );

  const items = notifications ?? [];
  const unreadCount = items.filter((item) => !seenIds.has(item.id)).length;

  function markSeen(id: string) {
    const next = new Set(seenIds);
    next.add(id);
    setSeenIds(next);
    saveSeenIds(next);
  }

  function markAllSeen() {
    const next = new Set(seenIds);
    items.forEach((item) => next.add(item.id));
    setSeenIds(next);
    saveSeenIds(next);
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : "Notifications"}
        className="relative rounded-md p-2 text-ink-600 hover:bg-surface-muted"
      >
        <Bell className="h-4 w-4" />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold leading-none text-white">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-md border border-surface-border bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-surface-border px-3 py-2">
            <p className="text-sm font-semibold text-ink-800">Notifications</p>
            {unreadCount > 0 && (
              <button type="button" onClick={markAllSeen} className="text-xs font-medium text-ink-500 hover:text-ink-800">
                Mark all as read
              </button>
            )}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {items.length === 0 ? (
              <p className="px-3 py-6 text-center text-sm text-ink-400">Nothing pending right now.</p>
            ) : (
              items.map((item) => {
                const isUnread = !seenIds.has(item.id);
                return (
                  <Link
                    key={item.id}
                    href={item.link}
                    onClick={() => {
                      markSeen(item.id);
                      setOpen(false);
                    }}
                    className={cn(
                      "block border-b border-surface-border px-3 py-2.5 last:border-b-0 hover:bg-surface-subtle",
                      isUnread && "bg-brand-50/60",
                    )}
                  >
                    <div className="flex items-start gap-2">
                      {isUnread && <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-600" />}
                      <div className={cn("min-w-0 flex-1", !isUnread && "pl-3.5")}>
                        <p className="text-sm font-medium text-ink-800">{item.title}</p>
                        <p className="mt-0.5 text-xs text-ink-500">{item.message}</p>
                        <p className="mt-1 text-[10px] uppercase tracking-wide text-ink-400">
                          {formatRelativeTime(item.occurredAt)}
                        </p>
                      </div>
                    </div>
                  </Link>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
