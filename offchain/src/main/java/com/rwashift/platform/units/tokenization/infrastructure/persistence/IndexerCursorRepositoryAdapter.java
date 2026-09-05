package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.IndexerCursor;
import com.rwashift.platform.units.tokenization.domain.repository.IndexerCursorRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class IndexerCursorRepositoryAdapter implements IndexerCursorRepository {

    private final IndexerCursorJpaRepository jpaRepository;

    IndexerCursorRepositoryAdapter(IndexerCursorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public IndexerCursor save(IndexerCursor cursor) {
        return jpaRepository.save(cursor);
    }

    @Override
    public Optional<IndexerCursor> findByContractAddress(String contractAddress) {
        return jpaRepository.findById(contractAddress);
    }
}
