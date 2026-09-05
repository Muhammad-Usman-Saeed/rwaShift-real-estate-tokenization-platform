package com.rwashift.platform.units.iam.api.rest;

import com.rwashift.platform.units.iam.api.dto.SignUpInvestorRequest;
import com.rwashift.platform.units.iam.api.dto.SignUpInvestorResponse;
import com.rwashift.platform.units.iam.application.service.UserProvisioningService;
import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Under {@code /api/v1/public/**} — the platform's only genuinely anonymous account-creation
 * path today (org-scoped users are still admin-provisioned; see {@code OrganizationUserController}).
 * Wallet sign-in needs no equivalent — it's self-provisioning by construction, see
 * {@code WalletAuthenticationProvider}.
 */
@RestController
@RequestMapping("/api/v1/public/investors")
public class InvestorSignUpController {

    private final UserProvisioningService userProvisioningService;

    public InvestorSignUpController(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpInvestorResponse> signUp(@Valid @RequestBody SignUpInvestorRequest request) {
        PlatformUser user = userProvisioningService.signUpInvestor(request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpInvestorResponse(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
