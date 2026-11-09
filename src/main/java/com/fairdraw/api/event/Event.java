package com.fairdraw.api.event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fairdraw.api.common.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID organizerId;
    private String title;
    private String venue;
    private OffsetDateTime startsAt;
    private OffsetDateTime entryOpensAt;
    private OffsetDateTime entryClosesAt;
    private OffsetDateTime paymentDueAt;
    private Integer price;
    private Integer ballotsPerUser;
    private Integer maxTeamSize;
    private String status = "draft";
    /** 허용된 다음 상태: draft→open→closed→drawn→paying→done 순서로만 */
    private static final Map<String, String> NEXT = Map.of(
            "draft", "open",
            "open", "closed",
            "closed", "drawn",
            "drawn", "paying",
            "paying", "done");

    public void transitionTo(String next) {
        if (!next.equals(NEXT.get(this.status))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSITION",
                    "공연 상태를 " + this.status + "에서 " + next + "(으)로 바꿀 수 없습니다");
        }
        this.status = next;
    }
}