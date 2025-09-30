package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.TicketConsumptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketConsumptionDetailRepository extends JpaRepository<TicketConsumptionDetail, Long> {

    // Buscar por el id del ticket padre
    List<TicketConsumptionDetail> findByTicketConsumption_Id(Long ticketId);

    // Buscar por el id del consumo
    List<TicketConsumptionDetail> findByConsumption_Id(Long consumptionId);

    // Buscar por el nombre del consumo
    List<TicketConsumptionDetail> findByConsumption_Name(String name);
}
