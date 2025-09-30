package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.TicketConsumptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketConsumptionDetailRepository extends JpaRepository<TicketConsumptionDetail,Long> {
    List<TicketConsumptionDetail> findByTicketId(Long ticketId);
    List<TicketConsumptionDetail> findByConsumptionId(Long consumptionId);
    List<TicketConsumptionDetail> findByConsumptionName(String name);
}
