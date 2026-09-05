package com.rwashift.platform.units.tokenization.api.rest;

import com.rwashift.platform.units.tokenization.api.dto.BlockchainTransactionResponse;
import com.rwashift.platform.units.tokenization.api.dto.ContractDeploymentResponse;
import com.rwashift.platform.units.tokenization.api.dto.DeployOfferingTokenRequest;
import com.rwashift.platform.units.tokenization.api.dto.OrganizationWalletResponse;
import com.rwashift.platform.units.tokenization.application.service.TokenizationApplicationService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokenization")
public class TokenizationController {

    private final TokenizationApplicationService tokenizationApplicationService;

    public TokenizationController(TokenizationApplicationService tokenizationApplicationService) {
        this.tokenizationApplicationService = tokenizationApplicationService;
    }

    @PostMapping("/offerings/{offeringId}/deploy")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<Void> deploy(@PathVariable String offeringId, @Valid @RequestBody DeployOfferingTokenRequest request) {
        tokenizationApplicationService.deployOfferingToken(offeringId, request.symbol());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/offerings/{offeringId}/deployments")
    public List<ContractDeploymentResponse> deployments(@PathVariable String offeringId) {
        return tokenizationApplicationService.getDeployments(offeringId).stream()
                .map(ContractDeploymentResponse::from)
                .toList();
    }

    /** Platform-wide deployments, for the admin Tokenization overview. */
    @GetMapping("/deployments")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<ContractDeploymentResponse> allDeployments() {
        return tokenizationApplicationService.listAllDeployments().stream()
                .map(ContractDeploymentResponse::from)
                .toList();
    }

    /** Platform-wide blockchain transaction ledger, for the admin Blockchain Transactions screen. */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<BlockchainTransactionResponse> transactions() {
        return tokenizationApplicationService.listTransactions().stream()
                .map(BlockchainTransactionResponse::from)
                .toList();
    }

    /** Resubmits a FAILED transaction — today, only the offering-tokenization "unpause" step. */
    @PostMapping("/transactions/{transactionId}/retry")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Void> retryTransaction(@PathVariable String transactionId) {
        tokenizationApplicationService.retryTransaction(transactionId);
        return ResponseEntity.accepted().build();
    }

    /** Every organization's on-chain wallet + live gas balance, for the admin "who needs funding" view. */
    @GetMapping("/organization-wallets")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<OrganizationWalletResponse> organizationWallets() {
        return tokenizationApplicationService.listOrganizationWallets().stream()
                // BigDecimal(unscaledValue, scale=18) is wei-to-ETH exactly, no division needed —
                // 1 ETH = 10^18 wei, so the wei amount IS the unscaled value at 18 decimal places.
                .map(w -> new OrganizationWalletResponse(w.organizationId(), w.walletAddress(),
                        new BigDecimal(w.balanceWei(), 18)))
                .toList();
    }
}
