package com.fairdraw.api.team;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.team.TeamDtos.*;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TeamService {
    public TeamResponse create(Jwt jwt, CreateTeamRequest req) { throw ApiException.notImplemented(); }
    public TeamResponse join(Jwt jwt, String code) { throw ApiException.notImplemented(); }
    public TeamResponse get(Jwt jwt, UUID teamId) { throw ApiException.notImplemented(); }
    public void leave(Jwt jwt, UUID teamId) { throw ApiException.notImplemented(); }
}