package com.rwashift.platform.units.legalstructure.domain.model;

import com.rwashift.platform.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * The legal ownership vehicle for an {@code Asset} (e.g. "Dubai Business Tower SPV Ltd."). V1
 * models this as informational/workflow metadata for the platform's process — it does not
 * itself create legal validity; that is established by the actual incorporation/registration
 * documents this entity references (see {@code units.document}).
 */
@Entity
@Table(name = "legal_structure")
public class LegalStructure extends TenantScopedEntity {

    @Column(name = "asset_id", nullable = false, length = 26)
    private String assetId;

    @Column(name = "legal_entity_name", nullable = false, length = 300)
    private String legalEntityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private LegalEntityType entityType;

    @Column(name = "jurisdiction", nullable = false, length = 100)
    private String jurisdiction;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_to_asset", nullable = false, length = 32)
    private RelationshipToAsset relationshipToAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "investment_instrument_type", nullable = false, length = 32)
    private InvestmentInstrumentType investmentInstrumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LegalStructureStatus status = LegalStructureStatus.DRAFT;

    protected LegalStructure() {
    }

    public LegalStructure(String organizationId, String assetId, String legalEntityName, LegalEntityType entityType,
            String jurisdiction, String registrationNumber, RelationshipToAsset relationshipToAsset,
            InvestmentInstrumentType investmentInstrumentType) {
        super(organizationId);
        this.assetId = assetId;
        this.legalEntityName = legalEntityName;
        this.entityType = entityType;
        this.jurisdiction = jurisdiction;
        this.registrationNumber = registrationNumber;
        this.relationshipToAsset = relationshipToAsset;
        this.investmentInstrumentType = investmentInstrumentType;
    }

    public void update(String legalEntityName, LegalEntityType entityType, String jurisdiction, String registrationNumber,
            RelationshipToAsset relationshipToAsset, InvestmentInstrumentType investmentInstrumentType) {
        this.legalEntityName = legalEntityName;
        this.entityType = entityType;
        this.jurisdiction = jurisdiction;
        this.registrationNumber = registrationNumber;
        this.relationshipToAsset = relationshipToAsset;
        this.investmentInstrumentType = investmentInstrumentType;
    }

    public void activate() {
        this.status = LegalStructureStatus.ACTIVE;
    }

    public void dissolve() {
        this.status = LegalStructureStatus.DISSOLVED;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public LegalEntityType getEntityType() {
        return entityType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public RelationshipToAsset getRelationshipToAsset() {
        return relationshipToAsset;
    }

    public InvestmentInstrumentType getInvestmentInstrumentType() {
        return investmentInstrumentType;
    }

    public LegalStructureStatus getStatus() {
        return status;
    }
}
