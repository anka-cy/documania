package com.documania.backend.ticket;

/** Interface de stockage des pièces jointes (abstraction Azure Blob / local). */
public interface AttachmentStorage {
    String store(String container, String blobName, byte[] content, String mimeType);
    byte[] load(String container, String blobName);
    void delete(String container, String blobName);
}