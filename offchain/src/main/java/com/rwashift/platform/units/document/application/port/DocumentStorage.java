package com.rwashift.platform.units.document.application.port;

/**
 * Abstraction over binary document storage. V1 ships {@code FilesystemDocumentStorage} for
 * local development; the interface is shaped for a drop-in S3-compatible replacement (put/get
 * by opaque key) without touching {@code DocumentApplicationService} or the domain model.
 */
public interface DocumentStorage {

    String store(String organizationId, String filename, byte[] content);

    byte[] retrieve(String storageKey);
}
