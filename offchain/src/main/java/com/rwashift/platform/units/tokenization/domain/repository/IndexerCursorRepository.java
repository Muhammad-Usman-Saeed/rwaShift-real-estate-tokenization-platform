package com.rwashift.platform.units.tokenization.domain.repository;

import com.rwashift.platform.units.tokenization.domain.model.IndexerCursor;
import java.util.Optional;

public interface IndexerCursorRepository {

    IndexerCursor save(IndexerCursor cursor);

    Optional<IndexerCursor> findByContractAddress(String contractAddress);
}
