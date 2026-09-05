const LEVELS = { debug: 10, info: 20, warn: 30, error: 40 } as const;
type Level = keyof typeof LEVELS;

/**
 * `NEXT_PUBLIC_LOG_LEVEL` so it's readable both server- and client-side (Next.js only inlines
 * `NEXT_PUBLIC_*` into the browser bundle). Defaults to "debug" outside production so local/demo
 * runs are verbose by default without needing extra config — matches the backend's
 * `logging.level.com.rwashift.platform` defaulting to DEBUG (see application.yml).
 */
const configuredLevel = (process.env.NEXT_PUBLIC_LOG_LEVEL as Level) || (process.env.NODE_ENV === "production" ? "info" : "debug");
const threshold = LEVELS[configuredLevel] ?? LEVELS.debug;

function log(level: Level, message: string, context?: Record<string, unknown>) {
  if (LEVELS[level] < threshold) return;
  const entry = { timestamp: new Date().toISOString(), level, message, ...context };
  const line = JSON.stringify(entry);
  if (level === "error") console.error(line);
  else if (level === "warn") console.warn(line);
  else console.log(line);
}

/** Structured console logging with level gating — see NEXT_PUBLIC_LOG_LEVEL above. */
export const logger = {
  debug: (message: string, context?: Record<string, unknown>) => log("debug", message, context),
  info: (message: string, context?: Record<string, unknown>) => log("info", message, context),
  warn: (message: string, context?: Record<string, unknown>) => log("warn", message, context),
  error: (message: string, context?: Record<string, unknown>) => log("error", message, context),
};
