/**
 * Mirrors the shape produced by the backend's `GlobalExceptionHandler` (Spring's native
 * `ProblemDetail`, RFC 9457 `application/problem+json`). Every field but `type`/`title`/`status`/
 * `detail` is an extension the backend adds — treat unknown extensions as optional.
 */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  traceId?: string;
  correlationId?: string;
  /** Present only on validation failures: field name -> message. */
  errors?: Record<string, string>;
}

const PROBLEM_TYPE_SUFFIXES = {
  notFound: "not-found",
  validation: "validation-error",
  stateConflict: "invalid-state-transition",
  domainRule: "domain-rule-violation",
  tenantDenied: "tenant-access-denied",
  malformedBody: "malformed-request-body",
} as const;

export type KnownProblemKind = keyof typeof PROBLEM_TYPE_SUFFIXES | "unauthorized" | "forbidden" | "network" | "unknown";

export function classifyProblem(problem: ProblemDetails | undefined, status: number): KnownProblemKind {
  if (!problem) {
    if (status === 401) return "unauthorized";
    if (status === 403) return "forbidden";
    return "unknown";
  }
  const type = problem.type ?? "";
  for (const [kind, suffix] of Object.entries(PROBLEM_TYPE_SUFFIXES)) {
    if (type.endsWith(suffix)) return kind as KnownProblemKind;
  }
  if (status === 401) return "unauthorized";
  if (status === 403) return "forbidden";
  return "unknown";
}

/**
 * Thrown by the API client for every non-2xx response. Carries the parsed Problem Details (when
 * the response body was one) so callers/UI can react to the specific failure kind rather than
 * pattern-matching strings.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly problem?: ProblemDetails;
  readonly kind: KnownProblemKind;

  constructor(status: number, problem: ProblemDetails | undefined, fallbackMessage: string) {
    super(problem?.detail ?? fallbackMessage);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
    this.kind = classifyProblem(problem, status);
  }

  get fieldErrors(): Record<string, string> | undefined {
    return this.problem?.errors;
  }

  get correlationId(): string | undefined {
    return this.problem?.correlationId ?? this.problem?.traceId;
  }
}

/**
 * Maps a known problem kind to a stable, user-safe title/message pair. Domain-rule and
 * not-found details already carry a specific, safe message from the backend (`problem.detail`) —
 * this table only supplies the *generic* fallback and the section heading, never overrides a
 * specific backend message with a vaguer one.
 */
const FRIENDLY_COPY: Record<KnownProblemKind, { title: string; message: string }> = {
  notFound: {
    title: "Not found",
    message: "The requested resource could not be found. It may have been removed or you may not have access to it.",
  },
  validation: {
    title: "Check the highlighted fields",
    message: "Some of the information provided did not pass validation.",
  },
  stateConflict: {
    title: "Action not available",
    message: "This action can't be performed in the resource's current state.",
  },
  domainRule: {
    title: "Request could not be completed",
    message: "This request violates a platform rule.",
  },
  tenantDenied: {
    title: "Access denied",
    message: "You don't have access to this organization's data.",
  },
  malformedBody: {
    title: "Request error",
    message: "The request could not be processed. Please try again.",
  },
  unauthorized: {
    title: "Sign in required",
    message: "Your session has expired. Please sign in again.",
  },
  forbidden: {
    title: "Access denied",
    message: "You don't have permission to perform this action.",
  },
  network: {
    title: "Connection problem",
    message: "Could not reach rwaShift. Check your connection and try again.",
  },
  unknown: {
    title: "Something went wrong",
    message: "An unexpected error occurred. Please try again, and contact support if it persists.",
  },
};

/** Domain-specific overrides for workflow failures that need a more precise, product-facing message. */
const DOMAIN_KEYWORD_OVERRIDES: Array<{ match: RegExp; title: string; message: string }> = [
  { match: /not been tokenized/i, title: "Offering not tokenized", message: "This offering has not completed on-chain tokenization yet." },
  { match: /not eligible|eligibility/i, title: "Not eligible", message: "This investor does not currently meet the eligibility requirements for this offering." },
  { match: /kyc/i, title: "Verification required", message: "KYC verification is required before this action can be completed." },
  { match: /payment/i, title: "Payment issue", message: "There was a problem with the payment for this investment." },
  { match: /revert|gas|nonce|insufficient funds/i, title: "Blockchain transaction failed", message: "The on-chain transaction failed or was reverted by the network." },
  { match: /rpc|connect.*node|timeout.*chain/i, title: "Blockchain connection issue", message: "Could not reach the blockchain network. Please try again shortly." },
];

export interface FriendlyError {
  title: string;
  message: string;
  correlationId?: string;
  fieldErrors?: Record<string, string>;
  kind: KnownProblemKind;
}

export function toFriendlyError(error: unknown): FriendlyError {
  if (error instanceof ApiError) {
    const backendDetail = error.problem?.detail;
    if (backendDetail) {
      const override = DOMAIN_KEYWORD_OVERRIDES.find((o) => o.match.test(backendDetail));
      if (override) {
        return { title: override.title, message: backendDetail, correlationId: error.correlationId, fieldErrors: error.fieldErrors, kind: error.kind };
      }
    }
    const copy = FRIENDLY_COPY[error.kind];
    return {
      title: copy.title,
      message: backendDetail && error.kind !== "unauthorized" ? backendDetail : copy.message,
      correlationId: error.correlationId,
      fieldErrors: error.fieldErrors,
      kind: error.kind,
    };
  }
  if (error instanceof Error && error.message === "Failed to fetch") {
    return { ...FRIENDLY_COPY.network, kind: "network" };
  }
  return { ...FRIENDLY_COPY.unknown, kind: "unknown" };
}
