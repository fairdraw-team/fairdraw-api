package com.fairdraw.api.event;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByOrganizerIdOrderByStartsAtAsc(UUID organizerId);

    List<Event> findByStatusOrderByStartsAtAsc(String status);

    List<Event> findByStatusNotOrderByStartsAtAsc(String status);

    /** 결제 열기 때 같은 공연을 동시에 두 번 열지 못하게 행을 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);
}