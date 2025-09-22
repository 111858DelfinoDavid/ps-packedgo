package com.packed_go.event_service.repositories.eventCategory;
import com.packed_go.event_service.entities.eventCategory.EventCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;



public interface EventCategoryRepository extends JpaRepository<EventCategoryEntity, Long> {
    Optional<EventCategoryEntity> findById(Long id);

    List<EventCategoryEntity> findByActiveIsTrue();

    Optional<EventCategoryEntity> findByName(String name);
}
