package com.fairdraw.api.team;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.entry.Entry;
import com.fairdraw.api.entry.EntryDtos.Allocation;
import com.fairdraw.api.entry.EntryRepository;
import com.fairdraw.api.entry.EntryService;
import com.fairdraw.api.entry.EntryStatus;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.profile.Profile;
import com.fairdraw.api.profile.ProfileRepository;
import com.fairdraw.api.profile.ProfileService;
import com.fairdraw.api.team.TeamDtos.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

    /** 헷갈리는 0, O, 1, I를 뺀 32글자 */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EntryRepository entryRepository;
    private final EntryService entryService;
    private final EventService eventService;
    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final SectionRepository sectionRepository;

    /** 팀 만들기: 팀 + 초대 코드 + 팀장 응모(고른 구역에 제비 전부) */
    @Transactional
    public TeamResponse create(Jwt jwt, CreateTeamRequest req) {
        Profile me = profileService.requireVerified(jwt);
        Event event = eventService.find(req.eventId());
        eventService.assertEntryWindowOpen(event);
        Section section = sectionRepository.findById(req.sectionId())
                .filter(s -> s.getEventId().equals(event.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ALLOCATION", "이 공연의 구역이 아닙니다"));
        if (entryRepository.existsByEventIdAndUserIdAndStatus(event.getId(), me.getId(), EntryStatus.ACTIVE)) {
            throw EntryService.alreadyEntered();
        }

        Team team = new Team();
        team.setEventId(event.getId());
        team.setSectionId(section.getId());
        team.setLeaderId(me.getId());
        team.setInviteCode(newInviteCode());
        teamRepository.save(team);
        teamMemberRepository.save(new TeamMember(team.getId(), me.getId()));
        entryService.createEntry(event, me.getId(), team.getId(),
                List.of(new Allocation(section.getId(), event.getBallotsPerUser())));
        return toResponse(team, me.getId());
    }

    /** 합류: 코드로 팀 찾기 → 중복·정원 검사 → 팀원 추가 + 같은 구역 응모 */
    @Transactional
    public TeamResponse join(Jwt jwt, String code) {
        Profile me = profileService.requireVerified(jwt);
        Team team = teamRepository.findByInviteCodeForUpdate(code.trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_CODE", "코드를 다시 확인하세요"));
        Event event = eventService.find(team.getEventId());
        eventService.assertEntryWindowOpen(event);
        if (entryRepository.existsByEventIdAndUserIdAndStatus(event.getId(), me.getId(), EntryStatus.ACTIVE)) {
            throw EntryService.alreadyEntered();
        }
        if (teamMemberRepository.countByTeamId(team.getId()) >= event.getMaxTeamSize()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TEAM_FULL", "이 팀은 정원이 찼습니다");
        }

        teamMemberRepository.save(new TeamMember(team.getId(), me.getId()));
        entryService.createEntry(event, me.getId(), team.getId(),
                List.of(new Allocation(team.getSectionId(), event.getBallotsPerUser())));
        return toResponse(team, me.getId());
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Jwt jwt, UUID teamId) {
        UUID me = UUID.fromString(jwt.getSubject());
        Team team = findTeamAsMember(teamId, me);
        return toResponse(team, me);
    }

    /** 탈퇴: 팀원이면 내 응모만 취소, 팀장이면 팀 해체 + 전원 응모 취소 */
    @Transactional
    public void leave(Jwt jwt, UUID teamId) {
        UUID me = UUID.fromString(jwt.getSubject());
        Team team = findTeamAsMember(teamId, me);
        eventService.assertEntryWindowOpen(eventService.find(team.getEventId()));

        if (team.getLeaderId().equals(me)) {
            // 응모가 팀을 가리키고 있으면 팀을 못 지우므로, 연결부터 끊음
            for (Entry e : entryRepository.findByTeamId(teamId)) {
                if (EntryStatus.ACTIVE.equals(e.getStatus())) {
                    entryService.cancel(e);
                }
                e.setTeamId(null);
            }
            teamMemberRepository.deleteByTeamId(teamId);
            teamRepository.delete(team);
        } else {
            entryRepository.findByEventIdAndUserIdAndStatus(team.getEventId(), me, EntryStatus.ACTIVE)
                    .ifPresent(e -> {
                        entryService.cancel(e);
                        e.setTeamId(null);
                    });
            teamMemberRepository.deleteById(new TeamMember.Key(teamId, me));
        }
    }

    // ===================== 내부 도우미 =====================

    private Team findTeamAsMember(UUID teamId, UUID me) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("팀"));
        if (!teamMemberRepository.existsByTeamIdAndProfileId(teamId, me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_TEAM_MEMBER", "팀원만 볼 수 있습니다");
        }
        return team;
    }

    /** 6글자 코드. 저장 전에 겹치는지 확인하고 겹치면 새로 뽑음 */
    private String newInviteCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!teamRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_GENERATION_FAILED", "잠시 후 다시 시도해 주세요");
    }

    private TeamResponse toResponse(Team team, UUID viewerId) {
        Event event = eventService.find(team.getEventId());
        String sectionName = sectionRepository.findById(team.getSectionId())
                .map(Section::getName).orElse(null);
        List<TeamMember> members = teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(team.getId());
        Map<UUID, String> names = profileRepository
                .findAllById(members.stream().map(TeamMember::getProfileId).toList()).stream()
                .collect(Collectors.toMap(Profile::getId,
                        p -> p.getRealName() == null ? "" : p.getRealName()));

        List<TeamMemberView> views = members.stream()
                .map(m -> new TeamMemberView(m.getProfileId(), names.get(m.getProfileId()),
                        m.getProfileId().equals(team.getLeaderId())))
                .toList();
        boolean isLeader = team.getLeaderId().equals(viewerId);
        return new TeamResponse(team.getId(), team.getEventId(), team.getSectionId(), sectionName,
                team.getLeaderId(), isLeader ? team.getInviteCode() : null,
                event.getMaxTeamSize(), views.size() >= event.getMaxTeamSize(), views);
    }
}