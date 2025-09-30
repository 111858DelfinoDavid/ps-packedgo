package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {


    List<Consumption> findByActiveIsTrue();

    Optional<Consumption> findByName(String name);
}
