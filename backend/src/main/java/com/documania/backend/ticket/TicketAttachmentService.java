package com.documania.backend.ticket;

import com.documania.backend.common.exception.ResourceNotFoundException;
import com.documania.backend.ticket.dto.TicketAttachmentResponse;
import com.documania.backend.user.UserAccount;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketAttachmentService {
    private static final String CONTAINER = "ticket-attachments";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_TYPES = List.of(
        "image/jpeg", "image/png",
        "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain", "text/csv");

    private final TicketAttachmentRepository attachmentRepository;
    private final AttachmentStorage storage;

    public TicketAttachmentService(TicketAttachmentRepository attachmentRepository, AttachmentStorage storage) {
        this.attachmentRepository = attachmentRepository;
        this.storage = storage;
    }

    @Transactional
    public TicketAttachmentResponse upload(Ticket ticket, MultipartFile file, UserAccount uploader) {
        validateFile(file);
        String blobName = ticket.getPublicId() + "/" + UUID.randomUUID() + "-" + sanitizeName(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Lecture du fichier impossible", e);
        }
        storage.store(CONTAINER, blobName, bytes, file.getContentType());
        TicketAttachment attachment = attachmentRepository.save(
            new TicketAttachment(ticket, file.getOriginalFilename(), blobName, file.getContentType(), bytes.length, uploader));
        return toResponse(attachment);
    }

    @Transactional
    public void linkToMessage(TicketMessage message, List<UUID> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;
        for (UUID publicId : attachmentIds) {
            TicketAttachment attachment = attachmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable : " + publicId));
            if (!attachment.getTicket().getId().equals(message.getTicket().getId())) {
                throw new IllegalArgumentException("La pièce jointe n'appartient pas à ce ticket");
            }
            attachment.linkToMessage(message);
            attachmentRepository.save(attachment);
        }
    }

    public ResponseEntity<Resource> download(UUID attachmentPublicId) {
        TicketAttachment attachment = attachmentRepository.findByPublicId(attachmentPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
        byte[] bytes = storage.load(CONTAINER, attachment.getBlobName());
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(attachment.getMimeType()))
            .contentLength(attachment.getFileSize())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(attachment.getFileName()).build().toString())
            .body(resource);
    }

    /** Récupère une pièce jointe (pour vérification d'appartenance avant téléchargement). */
    public TicketAttachment getAttachment(UUID attachmentPublicId) {
        return attachmentRepository.findByPublicId(attachmentPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("Le fichier est vide");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 10 Mo");
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("Type de fichier non autorisé : " + file.getContentType());
    }

    private String sanitizeName(String name) {
        return name == null ? "fichier" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private TicketAttachmentResponse toResponse(TicketAttachment att) {
        return new TicketAttachmentResponse(att.getPublicId(), att.getTicket().getPublicId(),
            att.getFileName(), att.getMimeType(), att.getFileSize(),
            att.getUploadedBy().getPublicId(), att.getUploadedBy().getEmail(), att.getCreatedAt());
    }
}