package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.TicketConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<TicketConsumption,Long> {
}
