/**
 * Formatting helpers for financial figures. The backend serializes {@code BigDecimal} fields as
 * JSON numbers (Jackson default) — safe for real-estate-scale amounts (well within
 * `Number.MAX_SAFE_INTEGER` at 2-4 decimal places), but never do arithmetic on these values in
 * the browser for anything that affects money owed/settled. All such calculations are
 * server-authoritative; the frontend only ever displays or previews them (see
 * `lib/validation/decimal.ts` for the one exception: live, non-authoritative preview math on the
 * Create Offering screen).
 */

export function formatCurrency(amount: number, currency = "USD", options?: Intl.NumberFormatOptions): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
    ...options,
  }).format(amount);
}

export function formatCompactCurrency(amount: number, currency = "USD"): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(amount);
}

export function formatNumber(value: number, options?: Intl.NumberFormatOptions): string {
  return new Intl.NumberFormat("en-US", options).format(value);
}

export function formatPercent(value: number, fractionDigits = 1): string {
  return `${value.toFixed(fractionDigits)}%`;
}

export function formatDate(value: string | Date, options?: Intl.DateTimeFormatOptions): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    ...options,
  }).format(date);
}

export function formatDateTime(value: string | Date): string {
  return formatDate(value, { hour: "2-digit", minute: "2-digit" });
}

export function formatRelativeTime(value: string | Date): string {
  const date = typeof value === "string" ? new Date(value) : value;
  const diffMs = date.getTime() - Date.now();
  const diffMinutes = Math.round(diffMs / 60000);
  const rtf = new Intl.RelativeTimeFormat("en-US", { numeric: "auto" });
  if (Math.abs(diffMinutes) < 60) return rtf.format(diffMinutes, "minute");
  const diffHours = Math.round(diffMinutes / 60);
  if (Math.abs(diffHours) < 24) return rtf.format(diffHours, "hour");
  const diffDays = Math.round(diffHours / 24);
  return rtf.format(diffDays, "day");
}

/** Truncates a 0x-prefixed address/hash to `0x1234…abcd` for compact display. */
export function truncateHex(value: string, lead = 6, trail = 4): string {
  if (value.length <= lead + trail + 2) return value;
  return `${value.slice(0, lead)}…${value.slice(-trail)}`;
}
