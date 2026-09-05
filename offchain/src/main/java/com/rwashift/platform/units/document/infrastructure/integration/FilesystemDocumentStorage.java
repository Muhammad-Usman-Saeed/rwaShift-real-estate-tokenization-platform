package com.rwashift.platform.units.document.infrastructure.integration;

import com.rwashift.platform.shared.domain.IdGenerator;
import com.rwashift.platform.units.document.application.port.DocumentStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local-development implementation of {@link DocumentStorage}, keyed by
 * {@code {organizationId}/{ulid}-{filename}} under a configurable root directory. A
 * production deployment swaps this for an S3-compatible adapter implementing the same
 * interface — nothing else in the platform changes.
 */
@Component
class FilesystemDocumentStorage implements DocumentStorage {

    private final Path rootPath;

    FilesystemDocumentStorage(@Value("${rwashift.documents.storage.root-path}") String rootPath) {
        this.rootPath = Path.of(rootPath);
    }

    @Override
    public String store(String organizationId, String filename, byte[] content) {
        try {
            Path orgDir = rootPath.resolve(organizationId);
            Files.createDirectories(orgDir);
            String storageKey = organizationId + "/" + IdGenerator.newId() + "-" + sanitize(filename);
            Files.write(rootPath.resolve(storageKey), content);
            return storageKey;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store document", e);
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(rootPath.resolve(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve document " + storageKey, e);
        }
    }

    private static String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
