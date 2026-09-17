package com.documania.backend.ticket;

import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CreateTicketRequest;
import com.documania.backend.ticket.dto.TicketAttachmentResponse;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.ticket.dto.TicketResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/client/tickets")
public class CustomerTicketController {
    private final CustomerTicketService ticketService;
    private final TicketStreamPublisher streamPublisher;

    public CustomerTicketController(CustomerTicketService ticketService,
                                    TicketStreamPublisher streamPublisher) {
        this.ticketService = ticketService;
        this.streamPublisher = streamPublisher;
    }

    @GetMapping
    public List<TicketResponse> listMine(Authentication authentication) {
        return ticketService.listForCurrentClient(authentication.getName());
    }

    @GetMapping("/{id}")
    public TicketDetailResponse detail(@PathVariable UUID id, Authentication authentication) {
        return ticketService.detailForCurrentClient(authentication.getName(), id);
    }

    /** Flux temps réel de la discussion : autorisation = même contrôle que le détail. */
    @GetMapping(value = "/{id}/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable UUID id, Authentication authentication) {
        ticketService.detailForCurrentClient(authentication.getName(), id);
        return streamPublisher.subscribeAsClient(id);
    }

    @PostMapping("/{id}/read")
    public TicketDetailResponse markRead(@PathVariable UUID id, Authentication authentication) {
        return ticketService.markRead(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetailResponse create(@Valid @RequestBody CreateTicketRequest request,
                                       Authentication authentication) {
        return ticketService.createForCurrentClient(authentication.getName(), request);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetailResponse addMessage(@PathVariable UUID id,
                                           @Valid @RequestBody AddTicketMessageRequest request,
                                           Authentication authentication) {
        TicketDetailResponse detail = ticketService.addClientMessage(authentication.getName(), id, request);
        // Service transactionnel déjà commité ici : le signal réveille les abonnés.
        List<TicketMessageResponse> messages = detail.messages();
        if (!messages.isEmpty()) {
            streamPublisher.publishNewMessage(id, messages.get(messages.size() - 1).publicId());
        }
        return detail;
    }

    @PatchMapping("/{id}/close")
    public TicketDetailResponse close(@PathVariable UUID id,
                                      @RequestParam(required = false) String reason,
                                      Authentication authentication) {
        return ticketService.closeForCurrentClient(authentication.getName(), id, reason);
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketAttachmentResponse uploadAttachment(@PathVariable UUID id,
                                                     @RequestParam("file") MultipartFile file,
                                                     Authentication authentication) {
        return ticketService.uploadAttachment(authentication.getName(), id, file);
    }

    @GetMapping("/{id}/attachments/{attId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(
            @PathVariable UUID id, @PathVariable UUID attId, Authentication authentication) {
        return ticketService.downloadAttachment(authentication.getName(), id, attId);
    }
}
