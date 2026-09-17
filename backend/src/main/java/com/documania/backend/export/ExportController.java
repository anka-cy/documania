package com.documania.backend.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exports XLSX (seul format implémenté) pour le portail staff. */
@RestController
@RequestMapping("/api/staff/export")
public class ExportController {

    private static final String XLSX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/clients")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_EXPORT')")
    public ResponseEntity<byte[]> exportClients() {
        return fileResponse("clients", exportService.exportClients());
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_EXPORT')")
    public ResponseEntity<byte[]> exportOrders() {
        return fileResponse("orders", exportService.exportOrders());
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SUBSCRIPTION_EXPORT')")
    public ResponseEntity<byte[]> exportSubscriptions() {
        return fileResponse("subscriptions", exportService.exportSubscriptions());
    }

    private ResponseEntity<byte[]> fileResponse(String baseName, byte[] content) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(baseName + ".xlsx")
                    .build()
                    .toString()
            )
            .body(content);
    }
}
