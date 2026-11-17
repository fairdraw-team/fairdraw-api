package com.fairdraw.api.draw;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        return toDrawInfo(drawnOrThrow(eventId));
    }

    /** 관객: 내 결과. 공연 없음 → 내 응모 없음 → 추첨 전 순서로 검사 */
    @Transactional(readOnly = true)
    public MyDrawResult myResult(Jwt jwt, UUID eventId) {
        eventService.find(eventId);                                         // 404 NOT_FOUND
        UUID userId = UUID.fromString(jwt.getSubject());
        Entry entry = entryRepository.findByEventIdAndUserIdAndStatus(eventId, userId, EntryStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", "아직 응모하지 않았습니다"));
        Draw draw = drawnOrThrow(eventId);                                  // 404 DRAW_NOT_FOUND
        DrawResult r = drawResultRepository.findByDrawIdAndEntryId(draw.getId(), entry.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DRAW_NOT_FOUND", "추첨 결과가 없습니다"));

        String sectionName = r.getSectionId() == null ? null
                : sectionRepository.findById(r.getSectionId()).map(Section::getName).orElse(null);
        List<UUID> myBallotIds = ballotRepository.findByEntryId(entry.getId()).stream()
                .map(Ballot::getId).toList();
        return new MyDrawResult(eventId, r.getResult(), r.getSectionId(), sectionName,
                r.getWaitlistRank(), entry.getTeamId(), myBallotIds);
    }

    /** 주최자: 결과 명단 (result를 빼면 전부) */
    @Transactional(readOnly = true)
    public DrawResultPage results(Jwt jwt, UUID eventId, String result, int page, int size) {
        eventService.findOwned(jwt, eventId);                               // 주최자 아니면 403
        Draw draw = drawnOrThrow(eventId);
        if (result != null && result.isBlank()) result = null;
        if (result != null && !List.of(DrawResultType.WON, DrawResultType.WAITLISTED, DrawResultType.LOST).contains(result)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "result는 won, waitlisted, lost 중 하나입니다");
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Object[]> p = drawResultRepository.findResultRows(draw.getId(), result,
                PageRequest.of(Math.max(page, 0), safeSize));

        List<DrawResultRow> rows = p.getContent().stream().map(r -> new DrawResultRow(
                UUID.fromString((String) r[0]),
                (String) r[1],
                maskPhone((String) r[2]),
                (String) r[3],
                (String) r[4],
                r[5] == null ? null : ((Number) r[5]).intValue(),
                r[6] == null ? null : UUID.fromString((String) r[6])
        )).toList();
        return new DrawResultPage(rows, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** 누구나: 추첨 순서대로 줄 선 제비 목록 (이름·번호·응모 id 없음) */
    @Transactional(readOnly = true)
    public PublicBallotsResponse publicBallots(UUID eventId) {
        Draw draw = drawnOrThrow(eventId);
        List<Entry> entries = entryRepository.findByEventIdAndStatus(eventId, EntryStatus.ACTIVE);
        List<Ballot> ballots = ballotRepository.findByEntryIdIn(entries.stream().map(Entry::getId).toList());
        Map<String, Ballot> byId = ballots.stream()
                .collect(Collectors.toMap(b -> b.getId().toString(), Function.identity()));

        // 저장된 시드로 순서를 다시 계산 (검증 페이지가 같은 계산을 다시 함)
        List<String> ordered = DrawEngine.orderOf(draw.getSeed(),
                ballots.stream().map(b -> b.getId().toString()).toList());

        // 결정한 제비 → 결과. 팀은 팀원 모두가 같은 제비라 여러 행이어도 값은 하나
        Map<UUID, String> resultByBallot = new HashMap<>();
        for (DrawResult r : drawResultRepository.findByDrawId(draw.getId())) {
            if (r.getBallotId() != null) resultByBallot.put(r.getBallotId(), r.getResult());
        }
        Map<UUID, String> sectionNames = sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .collect(Collectors.toMap(Section::getId, Section::getName));

        List<PublicBallot> list = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Ballot b = byId.get(ordered.get(i));
            list.add(new PublicBallot(i + 1, b.getId(), sectionNames.get(b.getSectionId()),
                    resultByBallot.get(b.getId())));
        }
        return new PublicBallotsResponse(eventId, list);
    }

    /** 추첨까지 끝난 draws 행. 봉인만 됐거나 없으면 404 DRAW_NOT_FOUND */
    private Draw drawnOrThrow(UUID eventId) {
        return drawRepository.findByEventId(eventId)
                .filter(d -> "drawn".equals(d.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DRAW_NOT_FOUND", "아직 추첨하지 않았습니다"));
    }

    /** 821012345678 → ***-****-5678 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "***-****-" + phone.substring(phone.length() - 4);
    }
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