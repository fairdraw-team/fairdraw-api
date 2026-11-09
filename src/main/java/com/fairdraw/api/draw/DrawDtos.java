package com.fairdraw.api.draw;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DrawDtos {

    /** 경쟁률 ratio = 응모 인원 / 정원 (소수점 한 자리) */
    public record SectionStat(UUID sectionId, String name, int capacity,
                              long applicants, long ballots, double ratio) {}

    public record StatsResponse(UUID eventId, String status, long totalApplicants, long totalBallots,
                                long teamCount, long cancelledCount, List<SectionStat> sections) {}

    public record SectionBallots(UUID sectionId, String name, long ballots) {}

    /** 공개 봉인 정보: 사람 이름·번호·응모 id는 절대 넣지 않음 */
    public record SealResponse(UUID eventId, String rosterHash, OffsetDateTime sealedAt,
                               int ballotCount, List<SectionBallots> sections) {}
}