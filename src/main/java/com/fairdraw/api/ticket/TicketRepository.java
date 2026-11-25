package com.fairdraw.api.ticket;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByBookingId(UUID bookingId);
    List<Ticket> findByBookingIdIn(Collection<UUID> bookingIds);
}