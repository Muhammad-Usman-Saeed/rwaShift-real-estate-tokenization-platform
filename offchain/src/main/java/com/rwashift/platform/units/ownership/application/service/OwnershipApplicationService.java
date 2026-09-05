package com.rwashift.platform.units.ownership.application.service;

import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import com.rwashift.platform.units.ownership.application.port.OwnershipLookupPort;
import com.rwashift.platform.units.ownership.application.port.OwnershipProjectionPort;
import com.rwashift.platform.units.ownership.application.port.OwnershipSnapshot;
import com.rwashift.platform.units.ownership.domain.model.OwnershipRecord;
import com.rwashift.platform.units.ownership.domain.repository.OwnershipRecordRepository;
import java.math.BigInteger;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the ownership read-model projection. THIS UNIT NEVER TOUCHES WEB3J and never
 * accepts a balance mutation from any API — the only write path is {@link #applyTransfer},
 * called exclusively by Tokenization's event indexer replaying confirmed on-chain
 * {@code Transfer} events (see {@link OwnershipRecord} for the "this is a projection" contract).
 */
@Service
public class OwnershipApplicationService implements OwnershipProjectionPort, OwnershipLookupPort {

    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    private final OwnershipRecordRepository ownershipRecordRepository;
    private final InvestorLookupPort investorLookupPort;
    private final AuditPort auditPort;

    public OwnershipApplicationService(OwnershipRecordRepository ownershipRecordRepository,
            InvestorLookupPort investorLookupPort, AuditPort auditPort) {
        this.ownershipRecordRepository = ownershipRecordRepository;
        this.investorLookupPort = investorLookupPort;
        this.auditPort = auditPort;
    }

    /** Actor is always {@code null} (system/indexer) — this is never triggered by a human request. */
    @Override
    @Transactional
    public void applyTransfer(String tokenAddress, String offeringId, String fromWallet, String toWallet,
            BigInteger amount, long blockNumber) {
        if (!ZERO_ADDRESS.equalsIgnoreCase(fromWallet)) {
            adjust(tokenAddress, offeringId, fromWallet, amount.negate(), blockNumber);
        }
        if (!ZERO_ADDRESS.equalsIgnoreCase(toWallet)) {
            adjust(tokenAddress, offeringId, toWallet, amount, blockNumber);
        }
        auditPort.record(AuditPort.AuditEntry.of(null, null, "OWNERSHIP_TRANSFER_INDEXED", "OwnershipRecord",
                offeringId, "from=" + fromWallet, "to=" + toWallet + ",amount=" + amount + ",block=" + blockNumber));
    }

    private void adjust(String tokenAddress, String offeringId, String walletAddress, BigInteger delta, long blockNumber) {
        OwnershipRecord record = ownershipRecordRepository.findByTokenAddressAndWalletAddress(tokenAddress, walletAddress)
                .orElseGet(() -> {
                    String investorId = investorLookupPort.findByWalletAddress(walletAddress)
                            .map(snapshot -> snapshot.investorId())
                            .orElse(null);
                    return new OwnershipRecord(investorId, offeringId, walletAddress, tokenAddress);
                });
        record.applyDelta(delta, blockNumber);
        ownershipRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<OwnershipRecord> getOwnershipForOffering(String offeringId) {
        return ownershipRecordRepository.findByOfferingId(offeringId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnershipSnapshot> findByOffering(String offeringId) {
        return ownershipRecordRepository.findByOfferingId(offeringId).stream()
                .map(r -> new OwnershipSnapshot(r.getInvestorId(), r.getWalletAddress(), r.getUnits()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OwnershipRecord> getOwnershipForInvestor(String investorId) {
        return ownershipRecordRepository.findByInvestorId(investorId);
    }
}
