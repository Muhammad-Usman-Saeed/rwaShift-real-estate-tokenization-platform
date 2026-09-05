package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.IndexerCursor;
import org.springframework.data.jpa.repository.JpaRepository;

interface IndexerCursorJpaRepository extends JpaRepository<IndexerCursor, String> {
}
