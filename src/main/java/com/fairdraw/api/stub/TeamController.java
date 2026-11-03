package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    /** 2주차: 팀 만들기. 만든 사람이 팀장이 된다. */
    @PostMapping
    public TeamResponse create(@RequestBody CreateTeamRequest req) {
        throw ApiException.notImplemented();
    }

    /** 2주차: 팀 정보 보기 */
    @GetMapping("/{teamId}")
    public TeamResponse get(@PathVariable UUID teamId) {
        throw ApiException.notImplemented();
    }

    /** 2주차: 휴대폰 번호로 팀원 추가 */
    @PostMapping("/{teamId}/members")
    public TeamResponse addMember(@PathVariable UUID teamId, @RequestBody AddMemberRequest req) {
        throw ApiException.notImplemented();
    }

    public record CreateTeamRequest(UUID eventId) {}
    public record AddMemberRequest(String phone) {}
    public record MemberResponse(UUID profileId, String realName, String phone) {}
    public record TeamResponse(UUID id, UUID eventId, UUID leaderId, List<MemberResponse> members) {}
}