import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusPill } from "./StatusPill";

describe("StatusPill", () => {
  it("renders the known label for a mapped status", () => {
    render(<StatusPill status="UNDER_REVIEW" />);
    expect(screen.getByText("Under Review")).toBeInTheDocument();
  });

  it("never conveys status through the label text alone missing", () => {
    render(<StatusPill status="CONFIRMED" />);
    // The dot is a decorative, non-text indicator (aria-hidden) — status must still be readable as text.
    expect(screen.getByText("Confirmed")).toBeInTheDocument();
  });

  it("falls back to a humanized label for an unmapped status", () => {
    render(<StatusPill status="SOME_NEW_STATUS" />);
    expect(screen.getByText("SOME NEW STATUS")).toBeInTheDocument();
  });
});
