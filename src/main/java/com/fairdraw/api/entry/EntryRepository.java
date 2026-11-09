package com.fairdraw.api.entry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, UUID> {
    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, String status);
    Optional<Entry> findByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, String status);
    List<Entry> findByTeamId(UUID teamId);
    long countByEventIdAndStatus(UUID eventId, String status);
    Page<Entry> findByEventIdOrderByCreatedAtAsc(UUID eventId, Pageable pageable);
}