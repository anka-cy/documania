package com.documania.backend.ticket;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Stockage Azure Blob pour la production (identité managée, ou connection-string en repli). */
@Component("azureBlobAttachmentStorage")
@ConditionalOnProperty(name = "app.storage.blob.enabled", havingValue = "true")
public class AzureBlobAttachmentStorage implements AttachmentStorage {
    private static final Logger log = LoggerFactory.getLogger(AzureBlobAttachmentStorage.class);

    private final String connectionString;
    private final String endpoint;
    private final String clientId;
    private BlobServiceClient serviceClient;

    public AzureBlobAttachmentStorage(
        @Value("${app.storage.blob.connection-string:}") String connectionString,
        @Value("${app.storage.blob.endpoint:}") String endpoint,
        @Value("${app.storage.blob.client-id:}") String clientId) {
        this.connectionString = connectionString;
        this.endpoint = endpoint;
        this.clientId = clientId;
    }

    @PostConstruct void init() {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        boolean usingManagedIdentity;
        if (isSet(connectionString)) {
            builder.connectionString(connectionString);
            usingManagedIdentity = false;
        } else if (isSet(endpoint)) {
            DefaultAzureCredentialBuilder credential = new DefaultAzureCredentialBuilder();
            if (isSet(clientId)) {
                credential.managedIdentityClientId(clientId);
            }
            builder.endpoint(endpoint).credential(credential.build());
            usingManagedIdentity = true;
        } else {
            log.warn("Azure Blob Storage non configuré (ni connection-string ni endpoint)");
            return;
        }
        try {
            serviceClient = builder.buildClient();
            log.info("Azure Blob Storage initialisé ({})",
                usingManagedIdentity ? "identité managée" : "connection-string");
        } catch (Exception e) {
            log.error("Azure Blob Storage non disponible : {}", e.getMessage());
            serviceClient = null;
        }
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private BlobContainerClient container(String name) {
        if (serviceClient == null) {
            throw new IllegalStateException("Azure Blob Storage non initialisé");
        }
        BlobContainerClient c = serviceClient.getBlobContainerClient(name);
        if (!c.exists()) c.create();
        return c;
    }

    @Override
    public String store(String container, String blobName, byte[] content, String mimeType) {
        BlobClient blob = container(container).getBlobClient(blobName);
        blob.upload(new ByteArrayInputStream(content), content.length, true);
        blob.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders().setContentType(mimeType));
        log.info("Uploaded blob: {}/{}", container, blobName);
        return blob.getBlobUrl();
    }

    @Override
    public byte[] load(String container, String blobName) {
        BlobClient blob = container(container).getBlobClient(blobName);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            blob.download(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Échec du téléchargement du blob: " + blobName, e);
        }
    }

    @Override
    public void delete(String container, String blobName) {
        container(container).getBlobClient(blobName).deleteIfExists();
    }
}