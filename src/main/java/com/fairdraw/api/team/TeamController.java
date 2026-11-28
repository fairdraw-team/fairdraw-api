package com.fairdraw.api.team;

import com.fairdraw.api.team.TeamDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateTeamRequest req) {
        return teamService.create(jwt, req);
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse join(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinTeamRequest req) {
        return teamService.join(jwt, req.code());
    }

        /** 내가 들어가 있는 팀 목록 (마이페이지용) */
    @GetMapping("/me")
    public List<MyTeamResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return teamService.mine(jwt);
    }

    @GetMapping("/{teamId}")
    public TeamResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
        return teamService.get(jwt, teamId);
    }

    @DeleteMapping("/{teamId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
        teamService.leave(jwt, teamId);
    }
}