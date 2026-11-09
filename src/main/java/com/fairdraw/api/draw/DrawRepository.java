package com.fairdraw.api.draw;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawRepository extends JpaRepository<Draw, UUID> {
    Optional<Draw> findByEventId(UUID eventId);
}