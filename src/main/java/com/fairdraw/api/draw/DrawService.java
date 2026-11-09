package com.fairdraw.api.draw;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.draw.DrawDtos.*;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class DrawService {
    public SealResponse seal(Jwt jwt, UUID eventId) { throw ApiException.notImplemented(); }
    public StatsResponse stats(Jwt jwt, UUID eventId) { throw ApiException.notImplemented(); }
    public SealResponse publicSeal(UUID eventId) { throw ApiException.notImplemented(); }
}