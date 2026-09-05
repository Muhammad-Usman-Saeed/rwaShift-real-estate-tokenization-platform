package com.rwashift.platform.units.iam.api.rest;

import com.rwashift.platform.shared.security.TemporaryPasswordGenerator;
import com.rwashift.platform.units.iam.api.dto.CreateOrganizationUserRequest;
import com.rwashift.platform.units.iam.api.dto.CreateOrganizationUserResponse;
import com.rwashift.platform.units.iam.api.dto.OrganizationUserResponse;
import com.rwashift.platform.units.iam.application.service.UserProvisioningService;
import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a Platform Admin provision the first login (ISSUER_ADMIN, COMPLIANCE_OFFICER, etc.) for an
 * organization once it's created — see {@code OrganizationController} for organization creation
 * itself, which this always happens after. Restricted to PLATFORM_ADMIN, same as organization
 * create/update/delete.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class OrganizationUserController {

    private final UserProvisioningService userProvisioningService;

    public OrganizationUserController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping
    public ResponseEntity<CreateOrganizationUserResponse> create(@PathVariable String organizationId,
            @Valid @RequestBody CreateOrganizationUserRequest request) {
        String temporaryPassword = TemporaryPasswordGenerator.generate();
        PlatformUser user = userProvisioningService.createOrganizationUser(
                request.email(), temporaryPassword, request.displayName(), organizationId, request.role());
        CreateOrganizationUserResponse response = new CreateOrganizationUserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), request.role().name(), temporaryPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<OrganizationUserResponse> list(@PathVariable String organizationId) {
        return userProvisioningService.listOrganizationUsers(organizationId).stream()
                .map(OrganizationUserResponse::from)
                .toList();
    }
}
