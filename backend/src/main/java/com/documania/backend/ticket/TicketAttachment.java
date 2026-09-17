package com.documania.backend.ticket;

import com.documania.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "message_id") private TicketMessage message;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "blob_name", nullable = false, length = 500) private String blobName;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by", nullable = false)
    private UserAccount uploadedBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    protected TicketAttachment() {}

    public TicketAttachment(Ticket ticket, String fileName, String blobName, String mimeType,
                            long fileSize, UserAccount uploadedBy) {
        this.ticket = ticket;
        this.fileName = fileName;
        this.blobName = blobName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist void generatePublicId() { if (publicId == null) publicId = UUID.randomUUID(); }

    public void linkToMessage(TicketMessage message) {
        this.message = message;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Ticket getTicket() { return ticket; }
    public TicketMessage getMessage() { return message; }
    public String getFileName() { return fileName; }
    public String getBlobName() { return blobName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public UserAccount getUploadedBy() { return uploadedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
