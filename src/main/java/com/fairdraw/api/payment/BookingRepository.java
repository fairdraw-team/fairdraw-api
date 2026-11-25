package com.fairdraw.api.payment;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByEventId(UUID eventId);
    List<Booking> findByUserIdAndStatus(UUID userId, String status);

    /** 이 공연에서 내 예매 중 가장 최근 것 */
    Optional<Booking> findFirstByEventIdAndUserIdOrderByCreatedAtDesc(UUID eventId, UUID userId);

    /** 결제 확인·만료 처리가 같은 예매를 동시에 바꾸지 못하게 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);
}