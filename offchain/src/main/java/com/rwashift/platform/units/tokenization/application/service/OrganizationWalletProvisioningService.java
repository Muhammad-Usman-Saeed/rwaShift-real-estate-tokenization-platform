package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.units.tokenization.domain.model.OrganizationWallet;
import com.rwashift.platform.units.tokenization.domain.repository.OrganizationWalletRepository;
import com.rwashift.platform.units.tokenization.infrastructure.security.OrganizationWalletEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/**
 * Get-or-create for one organization's on-chain signing wallet — the address that becomes
 * {@code issuerAdmin}/{@code TOKEN_AGENT} for that organization's offerings, replacing the single
 * platform-wide shared agent wallet (see docs/critical-analysis.md and {@link OrganizationWallet}).
 * Provisioning is lazy: the first offering an organization tokenizes triggers key generation, so
 * organizations that never tokenize anything never accumulate an unused wallet.
 *
 * <p>The generated wallet starts with zero native gas balance — it must be funded before it can
 * submit its first transaction. This is a deliberate, visible operational step (not hidden behind
 * an auto-funding faucet call) so a real deployment's ops process has to consciously decide how
 * organization wallets get funded, rather than the platform silently draining its own balance into
 * every new organization.
 */
@Service
class OrganizationWalletProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationWalletProvisioningService.class);

    private final OrganizationWalletRepository repository;
    private final OrganizationWalletEncryptionService encryptionService;

    OrganizationWalletProvisioningService(OrganizationWalletRepository repository,
            OrganizationWalletEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    Credentials getOrCreateWallet(String organizationId) {
        OrganizationWallet wallet = repository.findByOrganizationId(organizationId)
                .orElseGet(() -> provisionNewWallet(organizationId));
        String privateKeyHex = encryptionService.decrypt(wallet.getEncryptedPrivateKey());
        return Credentials.create(privateKeyHex);
    }

    private OrganizationWallet provisionNewWallet(String organizationId) {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            Credentials credentials = Credentials.create(keyPair);
            String privateKeyHex = Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPrivateKey(), 64);
            String encrypted = encryptionService.encrypt(privateKeyHex);
            OrganizationWallet wallet = new OrganizationWallet(organizationId, credentials.getAddress(), encrypted);
            OrganizationWallet saved = repository.save(wallet);
            log.info("Provisioned on-chain wallet {} for organization {} — it holds zero gas balance and must be "
                    + "funded before its first transaction.", saved.getWalletAddress(), organizationId);
            return saved;
        } catch (DataIntegrityViolationException raceLost) {
            // Another concurrent call already provisioned this organization's wallet (unique
            // constraint on organization_id) — use the one that won, not an error.
            return repository.findByOrganizationId(organizationId)
                    .orElseThrow(() -> raceLost);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision on-chain wallet for organization " + organizationId, e);
        }
    }
}
