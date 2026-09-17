package com.documania.backend.ticket;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stockage local pour le développement et les tests. */
@Component
@ConditionalOnMissingBean(name = "azureBlobAttachmentStorage")
public class LocalAttachmentStorage implements AttachmentStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalAttachmentStorage.class);
    private Path baseDir;

    @PostConstruct void init() {
        try {
            baseDir = Path.of("./uploads");
            Files.createDirectories(baseDir);
            log.info("Local storage initialized at {}", baseDir.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Cannot create ./uploads, falling back to /tmp/uploads: {}", e.getMessage());
            try {
                baseDir = Path.of("/tmp/uploads");
                Files.createDirectories(baseDir);
            } catch (IOException e2) {
                log.error("Failed to create fallback uploads directory; attachments will fail", e2);
                baseDir = null;
            }
        }
    }

    @Override
    public String store(String container, String blobName, byte[] content, String mimeType) {
        if (baseDir == null) throw new RuntimeException("Stockage non initialisé");
        try {
            Path dir = baseDir.resolve(container);
            Files.createDirectories(dir);
            Path file = dir.resolve(blobName);
            Files.createDirectories(file.getParent());
            Files.write(file, content);
            log.info("Stored locally: {}", file);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Échec du stockage local du fichier: " + blobName, e);
        }
    }

    @Override
    public byte[] load(String container, String blobName) {
        try {
            Path file = baseDir.resolve(container).resolve(blobName);
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Fichier introuvable: " + blobName, e);
        }
    }

    @Override
    public void delete(String container, String blobName) {
        try {
            Files.deleteIfExists(baseDir.resolve(container).resolve(blobName));
        } catch (IOException e) {
            log.warn("Échec de la suppression locale: {}", blobName, e);
        }
    }
}