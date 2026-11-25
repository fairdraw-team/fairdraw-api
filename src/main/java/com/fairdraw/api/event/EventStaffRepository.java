package com.fairdraw.api.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStaffRepository extends JpaRepository<EventStaff, UUID> {

    boolean existsByEventIdAndPhone(UUID eventId, String phone);

    /** 가입한 스태프: profile_id로 */
    Optional<EventStaff> findFirstByEventIdAndProfileId(UUID eventId, UUID profileId);

    /** 가입 전에 등록된 스태프: 전화번호로 */
    Optional<EventStaff> findFirstByEventIdAndPhone(UUID eventId, String phone);
}