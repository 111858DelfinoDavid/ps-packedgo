package com.packed_go.event_service.repositories;
import com.packed_go.event_service.entities.EventCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategoryEntity, Long> {
    Optional<EventCategoryEntity> findById(Long id);

    List<EventCategoryEntity> findByActiveIsTrue();

    Optional<EventCategoryEntity> findByName(String name);
}
