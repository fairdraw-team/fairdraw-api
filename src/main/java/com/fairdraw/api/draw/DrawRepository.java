package com.fairdraw.api.draw;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrawRepository extends JpaRepository<Draw, UUID> {

    Optional<Draw> findByEventId(UUID eventId);

    /** 추첨할 때만: 두 요청이 동시에 추첨하지 못하게 이 공연의 draws 행을 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Draw d where d.eventId = :eventId")
    Optional<Draw> findByEventIdForUpdate(@Param("eventId") UUID eventId);
}