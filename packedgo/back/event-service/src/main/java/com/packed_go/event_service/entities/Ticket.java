package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia al usuario que compró el ticket (viene de otro microservicio)
    @Column(nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false, unique = true)
    private Pass pass;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_consumption_id", nullable = false, unique = true)
    private TicketConsumption ticketConsumption;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean redeemed = false;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime purchasedAt;
    private LocalDateTime redeemedAt;

    @Version
    private Long version;

    // Constructor por defecto
    public Ticket() {}

    // Constructor para crear un ticket
    public Ticket(Long userId, Pass pass, TicketConsumption ticketConsumption) {
        this.userId = userId;
        this.pass = pass;
        this.ticketConsumption = ticketConsumption;
        this.active = true;
        this.redeemed = false;
        this.createdAt = LocalDateTime.now();
        this.purchasedAt = LocalDateTime.now();
    }
}
