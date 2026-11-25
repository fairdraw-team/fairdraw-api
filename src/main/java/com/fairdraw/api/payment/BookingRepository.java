package com.fairdraw.api.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByEventId(UUID eventId);

    /** 이 공연에서 내 예매 중 가장 최근 것 */
    Optional<Booking> findFirstByEventIdAndUserIdOrderByCreatedAtDesc(UUID eventId, UUID userId);
}