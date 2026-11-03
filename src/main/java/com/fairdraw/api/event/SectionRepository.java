package com.fairdraw.api.event;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findByEventIdOrderByNameAsc(UUID eventId);
    boolean existsByEventIdAndName(UUID eventId, String name);
}