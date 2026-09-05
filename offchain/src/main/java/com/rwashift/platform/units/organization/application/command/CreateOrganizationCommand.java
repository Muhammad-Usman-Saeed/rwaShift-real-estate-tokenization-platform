package com.rwashift.platform.units.organization.application.command;

public record CreateOrganizationCommand(String legalName, String displayName, String countryCode) {
}
