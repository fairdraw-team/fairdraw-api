package com.fairdraw.api.team;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMember.Key> {
    List<TeamMember> findByTeamIdOrderByJoinedAtAsc(UUID teamId);
    long countByTeamId(UUID teamId);
    boolean existsByTeamIdAndProfileId(UUID teamId, UUID profileId);
    void deleteByTeamId(UUID teamId);
    
    /** 내가 들어가 있는 팀 (최근에 들어간 순) */
    List<TeamMember> findByProfileIdOrderByJoinedAtDesc(UUID profileId);
}