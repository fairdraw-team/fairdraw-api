package com.fairdraw.api.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public class TeamDtos {

    public record CreateTeamRequest(@NotNull UUID eventId, @NotNull UUID sectionId) {}

    public record JoinTeamRequest(@NotBlank @Size(min = 6, max = 6) String code) {}

    public record TeamMemberView(UUID profileId, String realName, boolean leader) {}

    /** inviteCode는 팀장에게만 채워지고, 팀원에게는 null */
    public record TeamResponse(UUID id, UUID eventId, UUID sectionId, String sectionName,
                               UUID leaderId, String inviteCode, int maxSize, boolean full,
                               List<TeamMemberView> members) {}
}