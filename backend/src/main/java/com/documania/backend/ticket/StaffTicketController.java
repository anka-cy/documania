package com.documania.backend.ticket;

import com.documania.backend.ticket.dto.AddTicketMessageRequest;
import com.documania.backend.ticket.dto.CloseTicketRequest;
import com.documania.backend.ticket.dto.CreateTicketTaskRequest;
import com.documania.backend.ticket.dto.DeleteTicketRequest;
import com.documania.backend.ticket.dto.TicketAttachmentResponse;
import com.documania.backend.ticket.dto.TicketDetailResponse;
import com.documania.backend.ticket.dto.TicketMessageResponse;
import com.documania.backend.ticket.dto.TicketResponse;
import com.documania.backend.ticket.dto.UpdateTicketRequest;
import com.documania.backend.ticket.dto.UpdateTicketTaskRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/staff/tickets")
public class StaffTicketController {
    private final StaffTicketService ticketService;
    private final TicketStreamPublisher streamPublisher;

    public StaffTicketController(StaffTicketService ticketService,
                                 TicketStreamPublisher streamPublisher) {
        this.ticketService = ticketService;
        this.streamPublisher = streamPublisher;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_READ')")
    public List<TicketResponse> listAll(Authentication authentication) {
        return ticketService.listAll(authentication.getName());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_READ')")
    public TicketDetailResponse markRead(@PathVariable UUID id, Authentication authentication) {
        return ticketService.markRead(authentication.getName(), id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_READ')")
    public TicketDetailResponse detail(@PathVariable UUID id, Authentication authentication) {
        return ticketService.detail(authentication.getName(), id);
    }

    /** Flux temps réel de la discussion : autorisation = même contrôle que le détail. */
    @GetMapping(value = "/{id}/stream", produces = "text/event-stream")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_READ')")
    public SseEmitter stream(@PathVariable UUID id, Authentication authentication) {
        ticketService.detail(authentication.getName(), id);
        return streamPublisher.subscribeAsStaff(id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse addMessage(@PathVariable UUID id,
                                           @Valid @RequestBody AddTicketMessageRequest request,
                                           Authentication authentication) {
        TicketDetailResponse detail = ticketService.addStaffMessage(authentication.getName(), id, request);
        // Service transactionnel déjà commité ici : le signal réveille les abonnés.
        List<TicketMessageResponse> messages = detail.messages();
        if (!messages.isEmpty()) {
            streamPublisher.publishNewMessage(id, messages.get(messages.size() - 1).publicId());
        }
        return detail;
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateTicketRequest request,
                                       Authentication authentication) {
        return ticketService.updateTicket(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse addTask(@PathVariable UUID id,
                                        @Valid @RequestBody CreateTicketTaskRequest request,
                                        Authentication authentication) {
        return ticketService.addTask(authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse updateTask(@PathVariable UUID id, @PathVariable UUID taskId,
                                           @Valid @RequestBody UpdateTicketTaskRequest request,
                                           Authentication authentication) {
        return ticketService.updateTask(authentication.getName(), id, taskId, request);
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse deleteTask(@PathVariable UUID id, @PathVariable UUID taskId,
                                           Authentication authentication) {
        return ticketService.deleteTask(authentication.getName(), id, taskId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTicket(@PathVariable UUID id,
                             @Valid @RequestBody DeleteTicketRequest request,
                             Authentication authentication) {
        ticketService.deleteTicket(authentication.getName(), id, request.reason());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketDetailResponse closeTicket(@PathVariable UUID id,
                                            @Valid @RequestBody CloseTicketRequest request,
                                            Authentication authentication) {
        return ticketService.closeTicket(authentication.getName(), id, request.reason());
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_UPDATE')")
    public TicketAttachmentResponse uploadAttachment(@PathVariable UUID id,
                                                      @RequestParam("file") MultipartFile file,
                                                      Authentication authentication) {
        return ticketService.uploadAttachment(authentication.getName(), id, file);
    }

    @GetMapping("/{id}/attachments/{attId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('TICKET_READ')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id, @PathVariable UUID attId,
                                                       Authentication authentication) {
        return ticketService.downloadAttachment(authentication.getName(), id, attId);
    }
}
