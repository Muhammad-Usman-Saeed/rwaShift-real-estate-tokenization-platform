package com.rwashift.platform.units.investor.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.iam.application.port.InvestorLinkPort;
import com.rwashift.platform.units.iam.application.port.WalletIdentityLookupPort;
import com.rwashift.platform.units.investor.application.command.OnboardInvestorCommand;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import com.rwashift.platform.units.investor.application.port.InvestorSnapshot;
import com.rwashift.platform.units.investor.domain.model.Investor;
import com.rwashift.platform.units.investor.domain.model.InvestorStatus;
import com.rwashift.platform.units.investor.domain.model.LinkedWallet;
import com.rwashift.platform.units.investor.domain.policy.WalletAddressValidator;
import com.rwashift.platform.units.investor.domain.repository.InvestorRepository;
import com.rwashift.platform.units.investor.domain.repository.LinkedWalletRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestorApplicationService implements InvestorLookupPort {

    private final InvestorRepository investorRepository;
    private final LinkedWalletRepository linkedWalletRepository;
    private final InvestorLinkPort investorLinkPort;
    private final WalletIdentityLookupPort walletIdentityLookupPort;
    private final AuditPort auditPort;

    public InvestorApplicationService(InvestorRepository investorRepository,
            LinkedWalletRepository linkedWalletRepository, InvestorLinkPort investorLinkPort,
            WalletIdentityLookupPort walletIdentityLookupPort, AuditPort auditPort) {
        this.investorRepository = investorRepository;
        this.linkedWalletRepository = linkedWalletRepository;
        this.investorLinkPort = investorLinkPort;
        this.walletIdentityLookupPort = walletIdentityLookupPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public Investor onboardInvestor(OnboardInvestorCommand command) {
        if (investorRepository.findByUserId(command.userId()).isPresent()) {
            throw new DomainException("User %s already has an investor profile".formatted(command.userId()));
        }
        WalletAddressValidator.validate(command.primaryWalletAddress());
        if (linkedWalletRepository.existsByWalletAddress(command.primaryWalletAddress())) {
            throw new DomainException("Wallet address is already linked to another investor");
        }
        requireWalletNotOwnedByAnotherAccount(command.primaryWalletAddress(), command.userId());

        Investor investor = new Investor(command.userId(), command.investorType(), command.displayName(),
                command.countryCode(), command.dateOfBirth(), command.entityRegistrationNumber());
        investor.designatePrimaryWallet(command.primaryWalletAddress());
        investor = investorRepository.save(investor);

        linkedWalletRepository.save(new LinkedWallet(investor.getId(), command.primaryWalletAddress(), true));
        investorLinkPort.linkInvestor(command.userId(), investor.getId());
        auditPort.record(AuditPort.AuditEntry.of(command.userId(), null, "INVESTOR_ONBOARDED", "Investor",
                investor.getId(), null, investor.getStatus().name()));
        return investor;
    }

    /**
     * Links (or re-links) an investor's wallet after onboarding — the product spec treats wallet
     * linking as a step the investor may take separately from platform login/onboarding (section
     * 3/14), distinct from the wallet address collected at {@link #onboardInvestor}.
     *
     * <p>The collision check is by <em>ownership</em>, not by address-equals-current-primary: an
     * investor must be able to switch back to any wallet they themselves have used before (it's
     * still their own {@link LinkedWallet} row, just not the current primary), while a wallet that
     * belongs to a genuinely different investor stays rejected either way. Skipping the insert when
     * that row already exists also avoids tripping {@code investor_linked_wallet}'s global unique
     * constraint on {@code wallet_address} when re-linking an address this investor already has a
     * row for.
     */
    @Transactional
    public Investor linkWallet(String investorId, String walletAddress) {
        Investor investor = getInvestor(investorId);
        WalletAddressValidator.validate(walletAddress);
        Optional<LinkedWallet> existingLink = linkedWalletRepository.findByWalletAddress(walletAddress);
        if (existingLink.isPresent() && !existingLink.get().getInvestorId().equals(investorId)) {
            throw new DomainException("Wallet address is already linked to another investor");
        }
        requireWalletNotOwnedByAnotherAccount(walletAddress, investor.getUserId());
        String previousWallet = investor.getPrimaryWalletAddress();
        investor.designatePrimaryWallet(walletAddress);
        investor = investorRepository.save(investor);
        if (existingLink.isEmpty()) {
            linkedWalletRepository.save(new LinkedWallet(investorId, walletAddress, true));
        }
        auditPort.record(AuditPort.AuditEntry.of(investor.getUserId(), null, "WALLET_LINKED", "Investor", investorId,
                previousWallet, walletAddress));
        return investor;
    }

    @Transactional(readOnly = true)
    public Investor getInvestor(String investorId) {
        return investorRepository.findById(investorId)
                .orElseThrow(() -> new NotFoundException("Investor", investorId));
    }

    /**
     * Refuses to link a wallet that already signs a *different* account in — the mirror image of
     * {@code UserProvisioningService#findOrCreateWalletUser}'s investor-linked-wallet fallback.
     * Without this, "Link Wallet" could quietly hand one wallet address to two accounts: whichever
     * account it already logs into, and whichever investor profile links it here.
     */
    private void requireWalletNotOwnedByAnotherAccount(String walletAddress, String userId) {
        walletIdentityLookupPort.findUserIdByWallet(walletAddress)
                .filter(ownerId -> !ownerId.equals(userId))
                .ifPresent(ownerId -> {
                    throw new DomainException("Wallet address is already used to sign in to a different account");
                });
    }

    /** Platform-wide investor list — gated to PLATFORM_ADMIN/COMPLIANCE_OFFICER at the controller. */
    @Transactional(readOnly = true)
    public java.util.List<Investor> listInvestors() {
        return investorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvestorSnapshot> findSnapshot(String investorId) {
        return investorRepository.findById(investorId).map(InvestorApplicationService::toSnapshot);
    }

    /**
     * Resolves via {@code investor_linked_wallet} (every wallet ever linked, globally unique per
     * row) rather than {@code investor.primaryWalletAddress} (only the *current* one) — an
     * investor who has since switched wallets still owns their earlier one's history (on-chain
     * balances, prior sign-ins), and that earlier wallet should still resolve back to them, not
     * fall through to creating an orphan account. See {@code UserProvisioningService#findOrCreateWalletUser}.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<InvestorSnapshot> findByWalletAddress(String walletAddress) {
        return linkedWalletRepository.findByWalletAddress(walletAddress)
                .flatMap(linked -> investorRepository.findById(linked.getInvestorId()))
                .map(InvestorApplicationService::toSnapshot);
    }

    private static InvestorSnapshot toSnapshot(Investor i) {
        return new InvestorSnapshot(i.getId(), i.getUserId(), i.getDisplayName(), i.getCountryCode(),
                i.getPrimaryWalletAddress(), i.getStatus() == InvestorStatus.ACTIVE);
    }
}
