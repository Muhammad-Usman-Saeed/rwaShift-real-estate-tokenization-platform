import { describe, expect, it } from "vitest";
import { ApiError, classifyProblem, toFriendlyError } from "./problem-details";

describe("classifyProblem", () => {
  it("classifies by the problem type URI suffix", () => {
    expect(classifyProblem({ status: 422, type: "https://rwashift.com/problems/domain-rule-violation" }, 422)).toBe("domainRule");
    expect(classifyProblem({ status: 404, type: "https://rwashift.com/problems/not-found" }, 404)).toBe("notFound");
  });

  it("falls back to status-based classification when there's no problem body", () => {
    expect(classifyProblem(undefined, 401)).toBe("unauthorized");
    expect(classifyProblem(undefined, 403)).toBe("forbidden");
    expect(classifyProblem(undefined, 500)).toBe("unknown");
  });
});

describe("toFriendlyError", () => {
  it("prefers a backend detail message matched against a domain keyword override", () => {
    const error = new ApiError(422, { status: 422, detail: "Offering has not been tokenized yet" }, "fallback");
    const friendly = toFriendlyError(error);
    expect(friendly.title).toBe("Offering not tokenized");
    expect(friendly.message).toBe("Offering has not been tokenized yet");
  });

  it("surfaces field errors from a validation problem", () => {
    const error = new ApiError(
      400,
      { status: 400, type: "https://rwashift.com/problems/validation-error", errors: { name: "must not be blank" } },
      "fallback",
    );
    const friendly = toFriendlyError(error);
    expect(friendly.fieldErrors).toEqual({ name: "must not be blank" });
  });

  it("never surfaces a raw backend detail for unauthorized errors", () => {
    const error = new ApiError(401, { status: 401, detail: "some internal detail" }, "fallback");
    const friendly = toFriendlyError(error);
    expect(friendly.message).not.toBe("some internal detail");
  });
});
