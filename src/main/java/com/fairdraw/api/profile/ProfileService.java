package com.fairdraw.api.profile;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.common.Phones;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    /** 출입증을 보고 내 프로필을 찾거나 만든다. 출입증에 번호가 있으면 저장한다. */
    @Transactional
    public Profile syncFromToken(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> new Profile(userId));

        String tokenPhone = jwt.getClaimAsString("phone");
        if (tokenPhone != null && !tokenPhone.isBlank()) {
            String phone = Phones.normalize(tokenPhone);
            if (!phone.equals(profile.getPhone())) {
                profileRepository.findByPhone(phone)
                        .filter(other -> !other.getId().equals(userId))
                        .ifPresent(other -> {
                            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "이미 가입된 번호입니다");
                        });
                profile.setPhone(phone);
            }
        }
        return profileRepository.save(profile);
    }

    @Transactional
    public Profile updateRealName(Jwt jwt, String realName) {
        Profile profile = syncFromToken(jwt);
        profile.setRealName(realName.trim());
        return profile;
    }
}