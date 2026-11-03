package com.fairdraw.api.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

    @Id
    private UUID id;          // = Supabase 사용자 번호(sub)

    private String phone;     // 821012345678

    private String realName;  // 자동으로 real_name 칸과 연결됨

    private String role = "user";

    public Profile(UUID id) {
        this.id = id;
    }
}