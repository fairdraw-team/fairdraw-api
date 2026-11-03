package com.fairdraw.api.event;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStaffRepository extends JpaRepository<EventStaff, UUID> {
    boolean existsByEventIdAndPhone(UUID eventId, String phone);
}