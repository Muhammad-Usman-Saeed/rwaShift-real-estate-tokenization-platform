import { describe, expect, it } from "vitest";
import { formatCurrency, formatPercent, truncateHex } from "./format";

describe("formatCurrency", () => {
  it("formats whole dollar amounts without decimals", () => {
    expect(formatCurrency(2_000_000, "USD")).toBe("$2,000,000");
  });

  it("respects an explicit fraction digit override", () => {
    expect(formatCurrency(100, "USD", { maximumFractionDigits: 4 })).toBe("$100");
  });
});

describe("formatPercent", () => {
  it("formats with one decimal by default", () => {
    expect(formatPercent(20)).toBe("20.0%");
  });
});

describe("truncateHex", () => {
  it("truncates long hex strings to lead…trail", () => {
    expect(truncateHex("0x1234567890abcdef1234567890abcdef12345678")).toBe("0x1234…5678");
  });

  it("leaves short strings untouched", () => {
    expect(truncateHex("0x1234")).toBe("0x1234");
  });
});
