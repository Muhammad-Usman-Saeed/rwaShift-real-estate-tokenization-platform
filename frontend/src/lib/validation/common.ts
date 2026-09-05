import { z } from "zod";

/**
 * Shared decimal-money field validator. Accepts string or number input from a form field, coerces
 * to a JS number, and enforces the same shape the backend's `@DecimalMin`/`BigDecimal` scale
 * constraints expect — this is a UX convenience only; the backend independently re-validates
 * every request and remains authoritative (see product spec section 27/28).
 */
export function decimalField(options: { min?: number; maxFractionDigits?: number; message?: string } = {}) {
  const { min = 0.01, maxFractionDigits = 4 } = options;
  return z
    .union([z.string(), z.number()])
    .transform((val) => (typeof val === "string" ? val.trim() : val))
    .pipe(
      z.coerce
        .number({ invalid_type_error: "Enter a valid number" })
        .refine((val) => !Number.isNaN(val), "Enter a valid number")
        .refine((val) => val >= min, options.message ?? `Must be at least ${min}`)
        .refine((val) => {
          const parts = val.toString().split(".");
          return !parts[1] || parts[1].length <= maxFractionDigits;
        }, `Up to ${maxFractionDigits} decimal places allowed`),
    );
}

export const walletAddressSchema = z
  .string()
  .regex(/^0x[a-fA-F0-9]{40}$/, "Enter a valid 0x wallet address (42 characters)");

export const countryCodeSchema = z
  .string()
  .length(2, "Use a 2-letter ISO country code")
  .toUpperCase();

export const currencyCodeSchema = z
  .string()
  .length(3, "Use a 3-letter currency code (e.g. USD)")
  .toUpperCase();

export const positiveIntSchema = z.coerce
  .number({ invalid_type_error: "Enter a whole number" })
  .int("Must be a whole number")
  .positive("Must be greater than zero");
