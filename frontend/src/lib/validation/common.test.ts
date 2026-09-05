import { describe, expect, it } from "vitest";
import { decimalField, walletAddressSchema } from "./common";

describe("decimalField", () => {
  const schema = decimalField({ min: 0.01, maxFractionDigits: 4 });

  it("accepts a valid decimal string", () => {
    expect(schema.parse("100.5")).toBe(100.5);
  });

  it("rejects a value below the minimum", () => {
    expect(() => schema.parse("0")).toThrow();
  });

  it("rejects too many fraction digits", () => {
    expect(() => schema.parse("1.23456")).toThrow();
  });
});

describe("walletAddressSchema", () => {
  it("accepts a valid 0x-prefixed 40-hex-char address", () => {
    expect(() => walletAddressSchema.parse("0x111111111111111111111111111111111111111a")).not.toThrow();
  });

  it("rejects a malformed address", () => {
    expect(() => walletAddressSchema.parse("not-an-address")).toThrow();
  });
});
