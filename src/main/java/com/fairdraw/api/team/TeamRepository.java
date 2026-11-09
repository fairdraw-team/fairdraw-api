package com.fairdraw.api.team;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByInviteCode(String inviteCode);

    long countByEventId(UUID eventId);

    /** 합류할 때: 두 사람이 동시에 마지막 자리에 들어오지 못하게 팀 행을 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Team t where t.inviteCode = :code")
    Optional<Team> findByInviteCodeForUpdate(@Param("code") String code);
}