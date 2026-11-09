package com.fairdraw.api.team;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team_members")
@IdClass(TeamMember.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class TeamMember {

    @Id
    private UUID teamId;

    @Id
    private UUID profileId;

    private OffsetDateTime joinedAt = OffsetDateTime.now();

    public TeamMember(UUID teamId, UUID profileId) {
        this.teamId = teamId;
        this.profileId = profileId;
    }

    /** 두 칸짜리 기본 키 */
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID teamId;
        private UUID profileId;
    }
}