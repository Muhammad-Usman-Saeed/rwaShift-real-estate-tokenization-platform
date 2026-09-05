package com.rwashift.platform.units.iam.api.dto;

import com.rwashift.platform.units.iam.application.service.OrganizationUserView;

public record OrganizationUserResponse(String userId, String email, String displayName, String role, String status) {

    public static OrganizationUserResponse from(OrganizationUserView view) {
        return new OrganizationUserResponse(view.userId(), view.email(), view.displayName(),
                view.role().name(), view.status().name());
    }
}
