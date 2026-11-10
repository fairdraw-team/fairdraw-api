package com.fairdraw.api.draw;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.draw.DrawDtos.*;
import com.fairdraw.api.entry.BallotRepository;
import com.fairdraw.api.entry.EntryRepository;
import com.fairdraw.api.entry.EntryStatus;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.team.TeamRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final BallotRepository ballotRepository;
    private final EntryRepository entryRepository;
    private final TeamRepository teamRepository;
    private final EventService eventService;

    /** 마감 + 명단 봉인 (전부 한 트랜잭션) */
    @Transactional
    public SealResponse seal(Jwt jwt, UUID eventId) {
        Event event = eventService.findOwned(jwt, eventId);                       // ① 주최자만
        if (!"open".equals(event.getStatus())) {                                  // ② 응모 중인가
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_OPEN", "응모 중인 공연만 마감할 수 있습니다");
        }
        if (OffsetDateTime.now().isBefore(event.getEntryClosesAt())) {            // ③ 마감 시각이 지났나
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENTRY_WINDOW_NOT_ENDED",
                    "응모 마감 시각이 지나야 마감할 수 있습니다");
        }

        List<String> ids = ballotRepository.findActiveBallotIds(eventId, EntryStatus.ACTIVE).stream() // ④
                .map(UUID::toString)
                .sorted()                                                         //    글자 순서로 정렬
                .toList();

        Draw draw = new Draw();
        draw.setEventId(eventId);
        draw.setRosterHash(hashOf(ids));                                          // ⑤ SHA-256
        draw.setBallotCount(ids.size());
        draw.setSealedAt(OffsetDateTime.now());
        drawRepository.save(draw);                                                // ⑥ 봉인 기록

        event.transitionTo("closed");                                             // ⑦ 공연 상태 closed
        return toSealResponse(draw);
    }

    /** 누구나: 봉인 정보 (봉인 전이면 404) */
    @Transactional(readOnly = true)
    public SealResponse publicSeal(UUID eventId) {
        Draw draw = drawRepository.findByEventId(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SEAL_NOT_FOUND", "아직 봉인되지 않았습니다"));
        return toSealResponse(draw);
    }

    /** 주최자: 구역별 응모 인원·제비 수·경쟁률 */
    @Transactional(readOnly = true)
    public StatsResponse stats(Jwt jwt, UUID eventId) {
        Event event = eventService.findOwned(jwt, eventId);
        List<SectionStat> sections = ballotRepository.sectionStats(eventId, EntryStatus.ACTIVE).stream().map(r -> {
            int capacity = ((Number) r[2]).intValue();
            long applicants = ((Number) r[3]).longValue();
            long ballots = ((Number) r[4]).longValue();
            double ratio = capacity == 0 ? 0 : Math.round(applicants * 10.0 / capacity) / 10.0;
            return new SectionStat(UUID.fromString(r[0].toString()), (String) r[1],
                    capacity, applicants, ballots, ratio);
        }).toList();

        return new StatsResponse(eventId, event.getStatus(),
                entryRepository.countByEventIdAndStatus(eventId, EntryStatus.ACTIVE),
                sections.stream().mapToLong(SectionStat::ballots).sum(),
                teamRepository.countByEventId(eventId),
                entryRepository.countByEventIdAndStatus(eventId, EntryStatus.CANCELLED),
                sections);
    }

    /** 정렬된 제비 id → 줄바꿈으로 이어 붙임 → SHA-256 → 16진수 64글자. DB 없이 돌아감 */
    public static String hashOf(List<String> sortedIds) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", sortedIds).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private SealResponse toSealResponse(Draw draw) {
        List<SectionBallots> sections = ballotRepository.sectionStats(draw.getEventId(), EntryStatus.ACTIVE).stream()
                .map(r -> new SectionBallots(UUID.fromString(r[0].toString()), (String) r[1],
                        ((Number) r[4]).longValue()))
                .toList();
        return new SealResponse(draw.getEventId(), draw.getRosterHash(), draw.getSealedAt(),
                draw.getBallotCount(), sections);
    }
}