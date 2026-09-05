import { ApiError, type ProblemDetails } from "@/lib/errors/problem-details";
import { logger } from "@/lib/utils/logger";

/**
 * Server-side calls (route handlers, server components) hit the API's internal network address;
 * browser calls hit its public address. In local dev these are the same host — see .env.example.
 */
const API_BASE_URL = typeof window === "undefined" ? process.env.API_BASE_URL : process.env.NEXT_PUBLIC_API_BASE_URL;

export interface RequestOptions extends Omit<RequestInit, "body"> {
  /** Bearer access token from the current session (see `lib/auth/session.ts`). */
  token?: string;
  body?: unknown;
  /** Set when `body` is already a `FormData` instance (file uploads) — skips JSON encoding. */
  isFormData?: boolean;
}

function newCorrelationId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

/**
 * The one place every capability client funnels through. Attaches the bearer token and a
 * correlation id (propagated to the backend's own correlation-id/trace machinery — see
 * `shared.web.CorrelationIdFilter` — and echoed back on error responses), parses RFC 9457
 * Problem Details on failure, and throws a typed `ApiError` rather than a raw `Response`.
 *
 * Capability clients (lib/api/assets.ts etc.) are the only code that should call this — never
 * call `fetch` directly from a component.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, body, isFormData, headers, ...rest } = options;

  const correlationId = newCorrelationId();
  const requestHeaders = new Headers(headers);
  requestHeaders.set("X-Correlation-Id", correlationId);
  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  let requestBody: BodyInit | undefined;
  if (isFormData) {
    requestBody = body as FormData;
  } else if (body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
    requestBody = JSON.stringify(body);
  }

  const method = rest.method ?? "GET";
  const startedAt = Date.now();
  logger.debug("api request", { correlationId, method, path });

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: requestHeaders,
      body: requestBody,
      cache: "no-store",
    });
  } catch (cause) {
    logger.error("api request failed (network)", {
      correlationId,
      method,
      path,
      durationMs: Date.now() - startedAt,
      error: cause instanceof Error ? cause.message : String(cause),
    });
    throw new ApiError(0, undefined, cause instanceof Error ? cause.message : "Network request failed");
  }

  if (response.status === 204) {
    logger.debug("api response", { correlationId, method, path, status: 204, durationMs: Date.now() - startedAt });
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const isJson = contentType.includes("json");
  const payload = isJson ? await response.json().catch(() => undefined) : undefined;

  if (!response.ok) {
    logger.warn("api response error", {
      correlationId,
      method,
      path,
      status: response.status,
      durationMs: Date.now() - startedAt,
    });
    throw new ApiError(response.status, payload as ProblemDetails | undefined, response.statusText || "Request failed");
  }

  logger.debug("api response", { correlationId, method, path, status: response.status, durationMs: Date.now() - startedAt });
  return payload as T;
}

/** For binary responses (document download) where the body isn't JSON. */
export async function apiDownload(path: string, token?: string): Promise<{ blob: Blob; filename?: string }> {
  const headers = new Headers();
  headers.set("X-Correlation-Id", newCorrelationId());
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, { headers, cache: "no-store" });
  if (!response.ok) {
    throw new ApiError(response.status, undefined, response.statusText || "Download failed");
  }
  const disposition = response.headers.get("content-disposition");
  const filename = disposition?.match(/filename="?([^"]+)"?/)?.[1];
  return { blob: await response.blob(), filename };
}
