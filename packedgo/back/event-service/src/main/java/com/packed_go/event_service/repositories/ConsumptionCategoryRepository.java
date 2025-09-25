package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.ConsumptionCategoryEntity;
import com.packed_go.event_service.entities.EventCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionCategoryRepository extends JpaRepository<ConsumptionCategoryEntity,Long> {
    Optional<ConsumptionCategoryEntity> findById(Long id);

    List<ConsumptionCategoryEntity> findByActiveIsTrue();

    Optional<ConsumptionCategoryEntity> findByName(String name);
}
