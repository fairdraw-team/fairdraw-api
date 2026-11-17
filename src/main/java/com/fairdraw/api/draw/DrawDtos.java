package com.fairdraw.api.draw;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DrawDtos {

    // ===================== 2주차: 마감·봉인·현황 =====================

    /** 경쟁률 ratio = 응모 인원 / 정원 (소수점 한 자리) */
    public record SectionStat(UUID sectionId, String name, int capacity,
                              long applicants, long ballots, double ratio) {}

    public record StatsResponse(UUID eventId, String status, long totalApplicants, long totalBallots,
                                long teamCount, long cancelledCount, List<SectionStat> sections) {}

    public record SectionBallots(UUID sectionId, String name, long ballots) {}

    /** 공개 봉인 정보: 사람 이름·번호·응모 id는 절대 넣지 않음 */
    public record SealResponse(UUID eventId, String rosterHash, OffsetDateTime sealedAt,
                               int ballotCount, List<SectionBallots> sections) {}

    // ===================== 3주차: 추첨 =====================

    /** 공개 추첨 정보의 구역 한 줄 */
    public record SectionDrawStat(UUID sectionId, String name, int capacity,
                                  long ballots, long won, long waitlisted) {}

    /** 공개 추첨 정보 (추첨 성공 응답도 같은 모양) */
    public record DrawInfoResponse(UUID eventId, String rosterHash, OffsetDateTime sealedAt,
                                   int ballotCount, String seed, OffsetDateTime drawnAt,
                                   Long drawnAtMillis, List<SectionDrawStat> sections) {}

    /** 내 결과: result는 won / waitlisted / lost */
    public record MyDrawResult(UUID eventId, String result, UUID sectionId, String sectionName,
                               Integer waitlistRank, UUID teamId, List<UUID> ballotIds) {}

    /** 공개 제비 한 장 (이름·번호·응모 id 없음) */
    public record PublicBallot(int order, UUID ballotId, String sectionName, String result) {}

    public record PublicBallotsResponse(UUID eventId, List<PublicBallot> ballots) {}

    /** 주최자 결과 명단 한 줄 (전화번호는 뒤 4자리만) */
    public record DrawResultRow(UUID entryId, String realName, String phone, String sectionName,
                                String result, Integer waitlistRank, UUID teamId) {}

    /** 2주차 응모자 명단과 같은 페이지 모양 */
    public record DrawResultPage(List<DrawResultRow> content, int page, int size,
                                 long totalElements, int totalPages) {}
}