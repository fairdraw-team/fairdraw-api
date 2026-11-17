package com.fairdraw.api.draw;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.draw.DrawDtos.*;
import com.fairdraw.api.draw.DrawEngine.DrawBallot;
import com.fairdraw.api.draw.DrawEngine.Outcome;
import com.fairdraw.api.entry.Ballot;
import com.fairdraw.api.entry.BallotRepository;
import com.fairdraw.api.entry.Entry;
import com.fairdraw.api.entry.EntryRepository;
import com.fairdraw.api.entry.EntryStatus;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.team.TeamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawService {

    private final DrawRepository drawRepository;
    private final DrawResultRepository drawResultRepository;
    private final BallotRepository ballotRepository;
    private final EntryRepository entryRepository;
    private final SectionRepository sectionRepository;
    private final TeamRepository teamRepository;
    private final EventService eventService;

    @PersistenceContext
    private EntityManager entityManager;

    // ===================== 2주차: 마감·봉인·현황 =====================

    /** 마감 + 명단 봉인 (전부 한 트랜잭션) */
    @Transactional
    public SealResponse seal(Jwt jwt, UUID eventId) {
        Event event = eventService.findOwned(jwt, eventId);
        if (!"open".equals(event.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_OPEN", "응모 중인 공연만 마감할 수 있습니다");
        }
        if (OffsetDateTime.now().isBefore(event.getEntryClosesAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENTRY_WINDOW_NOT_ENDED",
                    "응모 마감 시각이 지나야 마감할 수 있습니다");
        }

        List<String> ids = ballotRepository.findActiveBallotIds(eventId, EntryStatus.ACTIVE).stream()
                .map(UUID::toString)
                .sorted()
                .toList();

        Draw draw = new Draw();
        draw.setEventId(eventId);
        draw.setRosterHash(hashOf(ids));
        draw.setBallotCount(ids.size());
        draw.setSealedAt(OffsetDateTime.now());
        drawRepository.save(draw);

        event.transitionTo("closed");
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

    /** 정렬된 제비 id → 줄바꿈으로 이어 붙임 → SHA-256 → 16진수 64글자 */
    public static String hashOf(List<String> sortedIds) {
        return DrawEngine.sha256Hex(String.join("\n", sortedIds));
    }

    private SealResponse toSealResponse(Draw draw) {
        List<SectionBallots> sections = ballotRepository.sectionStats(draw.getEventId(), EntryStatus.ACTIVE).stream()
                .map(r -> new SectionBallots(UUID.fromString(r[0].toString()), (String) r[1],
                        ((Number) r[4]).longValue()))
                .toList();
        return new SealResponse(draw.getEventId(), draw.getRosterHash(), draw.getSealedAt(),
                draw.getBallotCount(), sections);
    }

    // ===================== 3주차 BE-5: 추첨 실행 =====================

    @Transactional
    public DrawInfoResponse draw(Jwt jwt, UUID eventId) {
        // ① 주최자만
        Event event = eventService.findOwned(jwt, eventId);

        // ② 공연 상태
        if (List.of("drawn", "paying", "done").contains(event.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_DRAWN", "이미 추첨한 공연입니다");
        }
        if (!"closed".equals(event.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_CLOSED", "마감된 공연만 추첨할 수 있습니다");
        }

        // ③ draws 행을 잠그고, 메모리의 옛 값이 아니라 DB의 새 값으로 다시 확인
        Draw draw = drawRepository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_CLOSED", "마감된 공연만 추첨할 수 있습니다"));
        entityManager.refresh(draw);
        if ("drawn".equals(draw.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_DRAWN", "이미 추첨한 공연입니다");
        }

        // ④ 살아 있는 응모의 제비로 명단 해시를 다시 계산해 봉인 해시와 비교
        List<Entry> entries = entryRepository.findByEventIdAndStatus(eventId, EntryStatus.ACTIVE);
        List<Ballot> ballots = ballotRepository.findByEntryIdIn(entries.stream().map(Entry::getId).toList());
        List<String> sortedIds = ballots.stream().map(b -> b.getId().toString()).sorted().toList();
        if (!hashOf(sortedIds).equals(draw.getRosterHash())) {
            throw new ApiException(HttpStatus.CONFLICT, "ROSTER_MISMATCH", "봉인 뒤 명단이 바뀌어 추첨할 수 없습니다");
        }

        // ⑤ 추첨 시각(밀리초까지)과 시드
        OffsetDateTime drawnAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        String seed = DrawEngine.seedOf(draw.getRosterHash(), drawnAt.toInstant().toEpochMilli());

        // ⑥ 줄 세우기 → 원래 제비 정보로 되돌리기 → 판정
        Map<String, Ballot> byId = ballots.stream()
                .collect(Collectors.toMap(b -> b.getId().toString(), Function.identity()));
        List<DrawBallot> ordered = DrawEngine.orderOf(seed, sortedIds).stream()
                .map(byId::get)
                .map(b -> new DrawBallot(b.getId(), b.getEntryId(), b.getSectionId()))
                .toList();

        Map<UUID, Integer> capacity = sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .collect(Collectors.toMap(Section::getId, Section::getCapacity));

        // 묶음: 같은 team_id인 응모끼리 (팀이 아니면 자기 하나라 넣지 않음)
        Map<UUID, List<UUID>> byTeam = entries.stream()
                .filter(e -> e.getTeamId() != null)
                .collect(Collectors.groupingBy(Entry::getTeamId,
                        Collectors.mapping(Entry::getId, Collectors.toList())));
        Map<UUID, List<UUID>> groups = new HashMap<>();
        for (Entry e : entries) {
            if (e.getTeamId() != null) groups.put(e.getId(), byTeam.get(e.getTeamId()));
        }

        List<Outcome> outcomes = DrawEngine.run(ordered, capacity, groups);

        // ⑦ 결과 저장
        List<DrawResult> rows = outcomes.stream().map(o -> {
            DrawResult r = new DrawResult();
            r.setDrawId(draw.getId());
            r.setEntryId(o.entryId());
            r.setSectionId(o.sectionId());
            r.setResult(o.result());
            r.setWaitlistRank(o.waitlistRank());
            r.setBallotId(o.ballotId());
            r.setDrawOrder(o.drawOrder());
            return r;
        }).toList();
        drawResultRepository.saveAll(rows);

        // ⑧ 추첨 기록
        draw.setSeed(seed);
        draw.setDrawnAt(drawnAt);
        draw.setStatus("drawn");

        // ⑨ 공연 상태 drawn
        event.transitionTo("drawn");
        return toDrawInfo(draw);
    }

    // ===================== 3주차 BE-6: 조회 =====================

    /** 누구나: 공개 추첨 정보 (봉인만 됐거나 없으면 404) */
    @Transactional(readOnly = true)
    public DrawInfoResponse publicDraw(UUID eventId) {
        Draw draw = drawRepository.findByEventId(eventId)
                .filter(d -> "drawn".equals(d.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DRAW_NOT_FOUND", "아직 추첨하지 않았습니다"));
        return toDrawInfo(draw);
    }

    public MyDrawResult myResult(Jwt jwt, UUID eventId) { throw ApiException.notImplemented(); }

    public DrawResultPage results(Jwt jwt, UUID eventId, String result, int page, int size) {
        throw ApiException.notImplemented();
    }

    public PublicBallotsResponse publicBallots(UUID eventId) { throw ApiException.notImplemented(); }

    // ===================== 도우미 =====================

    /** 추첨 정보: 구역별 제비 수와 결과 수를 따로 세서 붙임 (한 번에 join하면 숫자가 부풀어짐) */
    private DrawInfoResponse toDrawInfo(Draw draw) {
        UUID eventId = draw.getEventId();

        Map<UUID, Long> ballotsBySection = new HashMap<>();
        for (Object[] r : ballotRepository.sectionStats(eventId, EntryStatus.ACTIVE)) {
            ballotsBySection.put(UUID.fromString(r[0].toString()), ((Number) r[4]).longValue());
        }

        List<DrawResult> results = drawResultRepository.findByDrawId(draw.getId());
        Map<UUID, Long> won = countBySection(results, DrawResultType.WON);
        Map<UUID, Long> waitlisted = countBySection(results, DrawResultType.WAITLISTED);

        List<SectionDrawStat> sections = sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .map(s -> new SectionDrawStat(s.getId(), s.getName(), s.getCapacity(),
                        ballotsBySection.getOrDefault(s.getId(), 0L),
                        won.getOrDefault(s.getId(), 0L),
                        waitlisted.getOrDefault(s.getId(), 0L)))
                .toList();

        Long millis = draw.getDrawnAt() == null ? null : draw.getDrawnAt().toInstant().toEpochMilli();
        return new DrawInfoResponse(eventId, draw.getRosterHash(), draw.getSealedAt(), draw.getBallotCount(),
                draw.getSeed(), draw.getDrawnAt(), millis, sections);
    }

    private static Map<UUID, Long> countBySection(List<DrawResult> results, String type) {
        return results.stream()
                .filter(r -> type.equals(r.getResult()) && r.getSectionId() != null)
                .collect(Collectors.groupingBy(DrawResult::getSectionId, Collectors.counting()));
    }
}