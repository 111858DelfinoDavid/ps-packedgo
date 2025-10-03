package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="category_id")
    private EventCategory category;

    private String name;
    private String description;
    private LocalDateTime eventDate;
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    private String status="ACTIVE";
    private Long createdBy;
    private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime updatedAt=LocalDateTime.now();
    private boolean active;

    // Nueva funcionalidad: Gestión de Pass
    @Column(nullable = false)
    private Integer totalPasses = 0;

    @Column(nullable = false)
    private Integer availablePasses = 0;

    @Column(nullable = false)
    private Integer soldPasses = 0;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pass> passes = new ArrayList<>();

    @Version
    private Long version;

    // Métodos de utilidad para gestión de Pass
    public void addPass(Pass pass) {
        this.passes.add(pass);
        this.totalPasses++;
        this.availablePasses++;
    }

    public void markPassAsSold() {
        this.soldPasses++;
        this.availablePasses--;
    }

    public boolean hasAvailablePasses() {
        return this.availablePasses > 0;
    }

}
