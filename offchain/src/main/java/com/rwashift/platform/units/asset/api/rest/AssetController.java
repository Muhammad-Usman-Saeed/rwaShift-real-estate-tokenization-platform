package com.rwashift.platform.units.asset.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.api.dto.AssetResponse;
import com.rwashift.platform.units.asset.api.dto.CreateAssetRequest;
import com.rwashift.platform.units.asset.api.dto.UpdateAssetRequest;
import com.rwashift.platform.units.asset.application.command.CreateAssetCommand;
import com.rwashift.platform.units.asset.application.command.UpdateAssetCommand;
import com.rwashift.platform.units.asset.application.service.AssetApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetApplicationService assetApplicationService;

    public AssetController(AssetApplicationService assetApplicationService) {
        this.assetApplicationService = assetApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ISSUER_OPERATOR','ORGANIZATION_ADMIN')")
    public ResponseEntity<AssetResponse> create(TenantContext tenantContext, @Valid @RequestBody CreateAssetRequest request) {
        var asset = assetApplicationService.createAsset(tenantContext, new CreateAssetCommand(
                request.name(), request.type(), request.description(), request.location(),
                request.valuation(), request.currency()));
        return ResponseEntity.created(URI.create("/api/v1/assets/" + asset.getId())).body(AssetResponse.from(asset));
    }

    @PutMapping("/{assetId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ISSUER_OPERATOR','ORGANIZATION_ADMIN')")
    public AssetResponse update(TenantContext tenantContext, @PathVariable String assetId,
            @Valid @RequestBody UpdateAssetRequest request) {
        var asset = assetApplicationService.updateAsset(tenantContext, assetId, new UpdateAssetCommand(
                request.name(), request.description(), request.location(), request.valuation(), request.currency()));
        return AssetResponse.from(asset);
    }

    @PostMapping("/{assetId}/submit-for-verification")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public AssetResponse submitForVerification(TenantContext tenantContext, @PathVariable String assetId) {
        return AssetResponse.from(assetApplicationService.markUnderVerification(tenantContext, assetId));
    }

    @PostMapping("/{assetId}/verify")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public AssetResponse verify(TenantContext tenantContext, @PathVariable String assetId) {
        return AssetResponse.from(assetApplicationService.markVerified(tenantContext, assetId));
    }

    @GetMapping("/{assetId}")
    public AssetResponse get(TenantContext tenantContext, @PathVariable String assetId) {
        return AssetResponse.from(assetApplicationService.getAsset(tenantContext, assetId));
    }

    @GetMapping
    public List<AssetResponse> list(TenantContext tenantContext) {
        return assetApplicationService.listAssets(tenantContext).stream().map(AssetResponse::from).toList();
    }
}
