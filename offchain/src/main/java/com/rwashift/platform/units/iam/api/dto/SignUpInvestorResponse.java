package com.rwashift.platform.units.iam.api.dto;

/** Deliberately never echoes the password back, even hashed. */
public record SignUpInvestorResponse(String userId, String email, String displayName) {
}
