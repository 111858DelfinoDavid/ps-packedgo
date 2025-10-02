package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consumptions")
@Getter
@Setter

public class Consumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ConsumptionCategory category;

    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean active;

    //Propiedades de navegación con otras tablas

    @OneToMany(mappedBy = "consumption", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    private List<TicketConsumptionDetail> ticketDetails = new ArrayList<>();

}
