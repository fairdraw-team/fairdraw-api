package com.fairdraw.api.event;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByOrganizerIdOrderByStartsAtAsc(UUID organizerId);
    List<Event> findByStatusOrderByStartsAtAsc(String status);
}