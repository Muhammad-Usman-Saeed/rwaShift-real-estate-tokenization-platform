package com.rwashift.platform.units.investor.domain.model;

import com.rwashift.platform.shared.domain.Auditable;
import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An investor is a platform-wide identity, NOT scoped to any single issuer organization — the
 * same investor can hold units across offerings from many different issuers, so unlike Asset/
 * Offering/LegalStructure this aggregate carries no {@code organizationId}. It is also NOT
 * simply a wallet: {@link #primaryWalletAddress} is validated, investor-owned data, separate
 * from the on-chain identity that {@code units.tokenization} registers on the investor's
 * behalf once KYC/eligibility clears (see {@code units.kyc}, {@code units.compliance}).
 */
@Entity
@Table(name = "investor")
public class Investor extends Auditable {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    /** The IAM platform-login this investor profile belongs to. */
    @Column(name = "user_id", nullable = false, unique = true, length = 26)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "investor_type", nullable = false, length = 32)
    private InvestorType investorType;

    @Column(name = "display_name", nullable = false, length = 300)
    private String displayName;

    @Column(name = "country", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "entity_registration_number", length = 100)
    private String entityRegistrationNumber;

    @Column(name = "primary_wallet_address", length = 42)
    private String primaryWalletAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvestorStatus status = InvestorStatus.ACTIVE;

    protected Investor() {
    }

    public Investor(String userId, InvestorType investorType, String displayName, String countryCode,
            LocalDate dateOfBirth, String entityRegistrationNumber) {
        this.userId = userId;
        this.investorType = investorType;
        this.displayName = displayName;
        this.countryCode = countryCode;
        this.dateOfBirth = dateOfBirth;
        this.entityRegistrationNumber = entityRegistrationNumber;
    }

    public void designatePrimaryWallet(String walletAddress) {
        this.primaryWalletAddress = walletAddress;
    }

    public void suspend() {
        this.status = InvestorStatus.SUSPENDED;
    }

    public void reactivate() {
        this.status = InvestorStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public InvestorType getInvestorType() {
        return investorType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEntityRegistrationNumber() {
        return entityRegistrationNumber;
    }

    public String getPrimaryWalletAddress() {
        return primaryWalletAddress;
    }

    public InvestorStatus getStatus() {
        return status;
    }
}
