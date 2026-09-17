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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Dernière lecture d'une discussion de ticket par un utilisateur (client ou staff).
 * Un non-lu = nombre de messages postés après cette date pour ce (ticket, utilisateur).
 */
@Entity
@Table(name = "ticket_read_marks")
public class TicketReadMark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "last_read_at", nullable = false) private LocalDateTime lastReadAt;

    protected TicketReadMark() {}

    public TicketReadMark(Ticket ticket, UserAccount user, LocalDateTime lastReadAt) {
        this.ticket = ticket;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    public void markRead(LocalDateTime time) {
        this.lastReadAt = time;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public UserAccount getUser() { return user; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
}
