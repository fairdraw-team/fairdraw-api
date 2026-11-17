package com.fairdraw.api.draw;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 응모 하나의 추첨 결과. 낙첨이면 sectionId·waitlistRank·ballotId·drawOrder가 null */
@Entity
@Table(name = "draw_results")
@Getter
@Setter
@NoArgsConstructor
public class DrawResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID drawId;
    private UUID entryId;
    private UUID sectionId;
    private String result;              // DrawResultType.WON / WAITLISTED / LOST
    @Column(name = "rank")              // DB 칸 이름은 rank, 자바에서는 waitlistRank로 씀
    private Integer waitlistRank;
    private UUID ballotId;              // 결정한 제비 (팀이면 팀원 모두 같은 값)
    private Integer drawOrder;          // 그 제비의 순서 (1부터)
    private OffsetDateTime createdAt = OffsetDateTime.now();   // NOT NULL이라 꼭 채움
}