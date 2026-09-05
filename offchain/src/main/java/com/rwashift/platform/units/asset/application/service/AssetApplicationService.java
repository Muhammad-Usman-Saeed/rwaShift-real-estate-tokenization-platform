package com.rwashift.platform.units.asset.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.command.CreateAssetCommand;
import com.rwashift.platform.units.asset.application.command.UpdateAssetCommand;
import com.rwashift.platform.units.asset.application.port.AssetLookupPort;
import com.rwashift.platform.units.asset.domain.model.Asset;
import com.rwashift.platform.units.asset.domain.model.AssetStatus;
import com.rwashift.platform.units.asset.domain.repository.AssetRepository;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetApplicationService implements AssetLookupPort {

    private final AssetRepository assetRepository;
    private final AuditPort auditPort;

    public AssetApplicationService(AssetRepository assetRepository, AuditPort auditPort) {
        this.assetRepository = assetRepository;
        this.auditPort = auditPort;
    }

    @Transactional
    public Asset createAsset(TenantContext caller, CreateAssetCommand command) {
        Asset asset = new Asset(caller.organizationId(), command.name(), command.type(), command.description(),
                command.location(), command.valuation(), command.currency());
        asset = assetRepository.save(asset);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "ASSET_CREATED",
                "Asset", asset.getId(), null, asset.getStatus().name()));
        return asset;
    }

    /** Editable only before verification — the creator (or org admin) can fix mistakes, but not rewrite a record compliance has already signed off on. */
    @Transactional
    public Asset updateAsset(TenantContext caller, String assetId, UpdateAssetCommand command) {
        Asset asset = getOwnedAsset(caller, assetId);
        if (asset.getStatus() == AssetStatus.VERIFIED || asset.getStatus() == AssetStatus.ARCHIVED) {
            throw new DomainException("Cannot edit asset '%s': it has already been verified.".formatted(asset.getName()));
        }
        asset.update(command.name(), command.description(), command.location(), command.valuation(), command.currency());
        asset = assetRepository.save(asset);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "ASSET_UPDATED",
                "Asset", assetId, null, asset.getStatus().name()));
        return asset;
    }

    @Transactional
    public Asset markUnderVerification(TenantContext caller, String assetId) {
        Asset asset = getOwnedAsset(caller, assetId);
        String previousStatus = asset.getStatus().name();
        asset.markUnderVerification();
        asset = assetRepository.save(asset);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "ASSET_MARKED_UNDER_VERIFICATION",
                "Asset", assetId, previousStatus, asset.getStatus().name()));
        return asset;
    }

    @Transactional
    public Asset markVerified(TenantContext caller, String assetId) {
        Asset asset = getOwnedAsset(caller, assetId);
        String previousStatus = asset.getStatus().name();
        asset.markVerified();
        asset = assetRepository.save(asset);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "ASSET_VERIFIED",
                "Asset", assetId, previousStatus, asset.getStatus().name()));
        return asset;
    }

    /** Investors may view any asset (backing an offering they might invest in); issuer-side callers only their own. */
    @Transactional(readOnly = true)
    public Asset getAsset(TenantContext caller, String assetId) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> new NotFoundException("Asset", assetId));
        if (!caller.hasRole("INVESTOR")) {
            caller.requireSameOrganization(asset.getOrganizationId());
        }
        return asset;
    }

    /** Platform admins see every organization's assets; issuer-side callers see only their own. */
    @Transactional(readOnly = true)
    public List<Asset> listAssets(TenantContext caller) {
        return caller.isPlatformAdmin() ? assetRepository.findAll() : assetRepository.findByOrganizationId(caller.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInOrganization(String assetId, String organizationId) {
        return assetRepository.existsByIdAndOrganizationId(assetId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyInOrganization(String organizationId) {
        return assetRepository.existsByOrganizationId(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findDisplayName(String assetId) {
        return assetRepository.findById(assetId).map(Asset::getName);
    }

    private Asset getOwnedAsset(TenantContext caller, String assetId) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> new NotFoundException("Asset", assetId));
        caller.requireSameOrganization(asset.getOrganizationId());
        return asset;
    }
}
