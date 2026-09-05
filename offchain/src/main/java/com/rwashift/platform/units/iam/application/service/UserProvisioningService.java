package com.rwashift.platform.units.iam.application.service;

import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.iam.application.port.InvestorLinkPort;
import com.rwashift.platform.units.iam.application.port.WalletIdentityLookupPort;
import com.rwashift.platform.units.iam.domain.model.IdentityProvider;
import com.rwashift.platform.units.iam.domain.model.OrganizationMembership;
import com.rwashift.platform.units.iam.domain.model.PlatformRole;
import com.rwashift.platform.units.iam.domain.model.PlatformUser;
import com.rwashift.platform.units.iam.domain.model.PlatformUserIdentity;
import com.rwashift.platform.units.iam.domain.repository.OrganizationMembershipRepository;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserIdentityRepository;
import com.rwashift.platform.units.iam.domain.repository.PlatformUserRepository;
import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.security.WalletIdentity;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for provisioning platform logins. Used by the seed-data loader and by
 * {@code PLATFORM_ADMIN}/{@code ORGANIZATION_ADMIN} onboarding APIs. Other units never create
 * a {@link PlatformUser} directly — they call this service (or, for investor linkage,
 * {@link #linkInvestor}, exposed cross-unit as {@link InvestorLinkPort}).
 *
 * <p>One account, several ways in: {@link PlatformUserIdentity} maps each login method (password
 * email, wallet address, eventually a Google subject) to exactly one {@code PlatformUser}, so a
 * given identifier — a specific wallet, a specific email — always resolves back to the same
 * account rather than silently creating a duplicate. See {@link #findOrCreateWalletUser}.
 */
@Service
public class UserProvisioningService implements InvestorLinkPort, WalletIdentityLookupPort {

    private final PlatformUserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final PlatformUserIdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditPort auditPort;
    private final InvestorLookupPort investorLookupPort;

    // @Lazy: Investor already depends on this unit's InvestorLinkPort (implemented by this same
    // class, below) to call back once onboarding completes — a plain constructor dependency in
    // both directions would be a real Spring bean cycle. Deferring resolution until first actual
    // use (i.e. the first wallet login that needs the investor-linked-wallet fallback) breaks it
    // without weakening any runtime behavior, same pattern as BlockchainTransactionManager.
    public UserProvisioningService(PlatformUserRepository userRepository,
            OrganizationMembershipRepository membershipRepository, PlatformUserIdentityRepository identityRepository,
            PasswordEncoder passwordEncoder, AuditPort auditPort, @Lazy InvestorLookupPort investorLookupPort) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditPort = auditPort;
        this.investorLookupPort = investorLookupPort;
    }

    @Transactional
    public PlatformUser createGlobalUser(String email, String rawPassword, String displayName, PlatformRole globalRole) {
        requireUniqueEmail(email);
        PlatformUser user = new PlatformUser(email, passwordEncoder.encode(rawPassword), displayName);
        user.assignGlobalRole(globalRole);
        user = userRepository.save(user);
        auditPort.record(AuditPort.AuditEntry.of(user.getId(), null, "USER_PROVISIONED", "PlatformUser",
                user.getId(), null, globalRole == null ? "NO_GLOBAL_ROLE" : globalRole.name()));
        return user;
    }

    @Transactional
    public PlatformUser createOrganizationUser(String email, String rawPassword, String displayName,
            String organizationId, PlatformRole organizationRole) {
        if (organizationRole == PlatformRole.PLATFORM_ADMIN || organizationRole == PlatformRole.INVESTOR) {
            throw new DomainException("Role %s is not organization-scoped".formatted(organizationRole));
        }
        requireUniqueEmail(email);
        PlatformUser user = new PlatformUser(email, passwordEncoder.encode(rawPassword), displayName);
        user = userRepository.save(user);
        membershipRepository.save(new OrganizationMembership(user.getId(), organizationId, organizationRole));
        auditPort.record(AuditPort.AuditEntry.of(user.getId(), organizationId, "ORGANIZATION_USER_PROVISIONED",
                "PlatformUser", user.getId(), null, organizationRole.name()));
        return user;
    }

    /** Platform admin's org-detail "who has access" view — joins membership role onto each user's own record. */
    @Transactional(readOnly = true)
    public List<OrganizationUserView> listOrganizationUsers(String organizationId) {
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(membership -> {
                    PlatformUser user = userRepository.findById(membership.getUserId())
                            .orElseThrow(() -> new DomainException("Membership references missing user " + membership.getUserId()));
                    return new OrganizationUserView(user.getId(), user.getEmail(), user.getDisplayName(),
                            membership.getRole(), user.getStatus());
                })
                .toList();
    }

    /**
     * Called only from {@code WalletAuthenticationProvider} once a wallet signature has already
     * been verified — never exposed as its own REST endpoint, since "I control this address" is
     * proven by the caller, not by anything this method itself checks. Idempotent: a returning
     * wallet always resolves to the same account, via the {@code (WALLET, address)} row in
     * {@link PlatformUserIdentity} rather than by re-deriving a synthetic email each time (a
     * wallet address is opaque to {@link PlatformUser}, which still needs a unique, non-null
     * email for its own unrelated reasons — the synthetic one exists purely to satisfy that
     * column, not as the actual account key anymore). Also resolves to an existing account if the
     * wallet was linked via the investor profile's separate "Link Wallet" action instead of ever
     * being used to sign in before — see the second backfill check below.
     */
    @Transactional
    public PlatformUser findOrCreateWalletUser(String walletAddress) {
        String normalizedAddress = WalletIdentity.normalize(walletAddress);
        var existingIdentity = identityRepository.findByProviderAndIdentifier(IdentityProvider.WALLET, normalizedAddress);
        if (existingIdentity.isPresent()) {
            return userRepository.findById(existingIdentity.get().getUserId())
                    .orElseThrow(() -> new DomainException("Identity references missing user " + existingIdentity.get().getUserId()));
        }

        // Backfill safety net: a wallet account created before this identity table existed has no
        // (WALLET, address) row yet, but does have the old synthetic-email account — recognize it
        // by that email once, and give it a proper identity row so every login after this one
        // takes the fast path above instead.
        String syntheticEmail = WalletIdentity.syntheticEmail(normalizedAddress);
        var legacyUser = userRepository.findByEmail(syntheticEmail);
        if (legacyUser.isPresent()) {
            identityRepository.save(new PlatformUserIdentity(legacyUser.get().getId(), IdentityProvider.WALLET, normalizedAddress));
            return legacyUser.get();
        }

        // Backfill safety net #2: an investor may have linked this exact wallet to their existing
        // account via the separate "Link Wallet" profile action (Investor.primaryWalletAddress /
        // LinkedWallet) rather than by ever signing in with it — that's still the same real-world
        // wallet, so wallet sign-in must resolve back to that same account, not fork off a second,
        // empty one with no investor profile.
        var linkedInvestor = investorLookupPort.findByWalletAddress(normalizedAddress);
        if (linkedInvestor.isPresent() && linkedInvestor.get().userId() != null) {
            PlatformUser owner = userRepository.findById(linkedInvestor.get().userId())
                    .orElseThrow(() -> new DomainException("Investor references missing user " + linkedInvestor.get().userId()));
            identityRepository.save(new PlatformUserIdentity(owner.getId(), IdentityProvider.WALLET, normalizedAddress));
            return owner;
        }

        String unusablePassword = UUID.randomUUID() + UUID.randomUUID().toString();
        PlatformUser user = new PlatformUser(syntheticEmail, passwordEncoder.encode(unusablePassword),
                "Wallet " + WalletIdentity.shortAddress(normalizedAddress));
        user.assignGlobalRole(PlatformRole.INVESTOR);
        PlatformUser saved = userRepository.save(user);
        identityRepository.save(new PlatformUserIdentity(saved.getId(), IdentityProvider.WALLET, normalizedAddress));
        auditPort.record(AuditPort.AuditEntry.of(saved.getId(), null, "WALLET_USER_PROVISIONED", "PlatformUser",
                saved.getId(), null, normalizedAddress));
        return saved;
    }

    /**
     * The reverse of the {@link #findOrCreateWalletUser} backfill: lets Investor check, before
     * linking a wallet to an investor profile, whether that wallet already signs in to a
     * *different* account — see {@link WalletIdentityLookupPort}.
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> findUserIdByWallet(String walletAddress) {
        String normalizedAddress = WalletIdentity.normalize(walletAddress);
        return identityRepository.findByProviderAndIdentifier(IdentityProvider.WALLET, normalizedAddress)
                .map(PlatformUserIdentity::getUserId);
    }

    /**
     * Public self-signup for investors — email/password, no admin involved. Mirrors
     * {@link #findOrCreateWalletUser}'s "one account, several ways in" model: also records a
     * {@code (LOCAL, email)} identity row, so this account is uniformly represented alongside any
     * wallet identity it might link later. Unlike {@link #createOrganizationUser}, this always
     * assigns the {@code INVESTOR} global role and never touches organization membership.
     */
    @Transactional
    public PlatformUser signUpInvestor(String email, String rawPassword, String displayName) {
        requireUniqueEmail(email);
        PlatformUser user = new PlatformUser(email, passwordEncoder.encode(rawPassword), displayName);
        user.assignGlobalRole(PlatformRole.INVESTOR);
        PlatformUser saved = userRepository.save(user);
        identityRepository.save(new PlatformUserIdentity(saved.getId(), IdentityProvider.LOCAL, email));
        auditPort.record(AuditPort.AuditEntry.of(saved.getId(), null, "INVESTOR_SELF_SIGNUP", "PlatformUser",
                saved.getId(), null, null));
        return saved;
    }

    /** Called by the Investor unit once onboarding completes (see {@link InvestorLinkPort}). */
    @Override
    @Transactional
    public void linkInvestor(String userId, String investorId) {
        PlatformUser user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Unknown user " + userId));
        user.linkInvestor(investorId);
        user.assignGlobalRole(PlatformRole.INVESTOR);
        userRepository.save(user);
        auditPort.record(AuditPort.AuditEntry.of(userId, null, "USER_LINKED_TO_INVESTOR", "PlatformUser", userId,
                null, investorId));
    }

    private void requireUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DomainException("Email already registered: " + email);
        }
    }
}
