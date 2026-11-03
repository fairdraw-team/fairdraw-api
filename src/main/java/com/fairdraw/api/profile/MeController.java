package com.fairdraw.api.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final ProfileService profileService;

    @GetMapping
    public MeResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return MeResponse.from(profileService.syncFromToken(jwt));
    }

    @PatchMapping
    public MeResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody UpdateMeRequest request) {
        return MeResponse.from(profileService.updateRealName(jwt, request.realName()));
    }

    public record MeResponse(UUID id, String phone, String realName, String role) {
        static MeResponse from(Profile p) {
            return new MeResponse(p.getId(), p.getPhone(), p.getRealName(), p.getRole());
        }
    }

    public record UpdateMeRequest(@NotBlank @Size(max = 30) String realName) {}
}