package com.documania.backend.client;

import com.documania.backend.client.dto.ClientResponse;
import com.documania.backend.client.dto.CreateClientRequest;
import com.documania.backend.client.dto.UpdateClientRequest;
import com.documania.backend.common.dto.DeleteReasonRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_CREATE')")
    public ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        return ClientMapper.toResponse(clientService.createClient(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_READ')")
    public List<ClientResponse> listActiveClients() {
        List<ClientResponse> responses = new ArrayList<>();
        for (Client client : clientService.listActiveClients()) {
            responses.add(ClientMapper.toResponse(client));
        }
        return responses;
    }

    @GetMapping("/archived")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_READ')")
    public List<ClientResponse> listArchivedClients() {
        List<ClientResponse> responses = new ArrayList<>();
        for (Client client : clientService.listArchivedClients()) {
            responses.add(ClientMapper.toResponse(client));
        }
        return responses;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_READ')")
    public ClientResponse findClient(@PathVariable UUID id) {
        return ClientMapper.toResponse(clientService.findClientById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_UPDATE')")
    public ClientResponse updateClient(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        return ClientMapper.toResponse(clientService.updateClient(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_ARCHIVE')")
    public ClientResponse archiveClient(@PathVariable UUID id) {
        return ClientMapper.toResponse(clientService.archiveClient(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CLIENT_RESTORE')")
    public ClientResponse restoreClient(@PathVariable UUID id) {
        return ClientMapper.toResponse(clientService.restoreClient(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteArchivedClient(
        @PathVariable UUID id,
        @Valid @RequestBody DeleteReasonRequest request
    ) {
        clientService.deleteArchivedClient(id, request.reason());
    }
}
