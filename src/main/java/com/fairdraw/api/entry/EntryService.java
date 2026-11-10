package com.fairdraw.api.entry;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.entry.EntryDtos.*;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.profile.Profile;
import com.fairdraw.api.profile.ProfileRepository;
import com.fairdraw.api.profile.ProfileService;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final BallotRepository ballotRepository;
    private final SectionRepository sectionRepository;
    private final EventService eventService;
    private final ProfileService profileService;
    private final ProfileRepository profileRepository;

    // ===================== 관객 =====================

    @Transactional
    public MyEntryResponse create(Jwt jwt, UUID eventId, CreateEntryRequest req) {
        Profile me = profileService.requireVerified(jwt);          // ① 휴대폰·실명
        Event event = eventService.find(eventId);
        eventService.assertEntryWindowOpen(event);                  // ② open인가 ③ 기간 안인가
        Entry entry = createEntry(event, me.getId(), null, req.allocations()); // ④ 제비 검사 ⑤ 중복 검사
        return toMyResponse(entry);
    }

    @Transactional(readOnly = true)
    public MyEntryResponse getMine(Jwt jwt, UUID eventId) {
        return toMyResponse(findMyActive(jwt, eventId));
    }

    @Transactional
    public void cancelMine(Jwt jwt, UUID eventId) {
        Entry entry = findMyActive(jwt, eventId);
        eventService.assertEntryWindowOpen(eventService.find(eventId));
        if (entry.getTeamId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IN_TEAM", "팀 응모는 팀 화면에서 탈퇴하세요");
        }
        cancel(entry);
    }

    // ===================== 주최자 =====================

    @Transactional(readOnly = true)
    public EntryPage listForOrganizer(Jwt jwt, UUID eventId, int page, int size) {
        eventService.findOwned(jwt, eventId);                       // 주최자가 아니면 403
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Entry> result = entryRepository.findByEventIdOrderByCreatedAtAsc(
                eventId, PageRequest.of(Math.max(page, 0), safeSize));
        List<Entry> entries = result.getContent();

        Map<UUID, Profile> profiles = profileRepository
                .findAllById(entries.stream().map(Entry::getUserId).toList()).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        Map<UUID, List<Ballot>> ballotsByEntry = ballotRepository
                .findByEntryIdIn(entries.stream().map(Entry::getId).toList()).stream()
                .collect(Collectors.groupingBy(Ballot::getEntryId));
        Map<UUID, String> sectionNames = sectionNames(eventId);

        List<EntryRow> rows = entries.stream().map(e -> {
            Profile p = profiles.get(e.getUserId());
            List<Ballot> bs = ballotsByEntry.getOrDefault(e.getId(), List.of());
            return new EntryRow(e.getId(),
                    p != null ? p.getRealName() : null,
                    maskPhone(p != null ? p.getPhone() : null),
                    summarize(bs, sectionNames), bs.size(),
                    e.getStatus(), e.getTeamId(), e.getCreatedAt());
        }).toList();

        return new EntryPage(rows, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    // ===================== 팀 서비스도 같이 쓰는 것 =====================

    /** ④ 제비 나누기 검사 → ⑤ 중복 검사 → 응모 1행 + 제비 N행 저장 */
    public Entry createEntry(Event event, UUID userId, UUID teamId, List<Allocation> allocations) {
        validateAllocations(event, allocations);
        if (entryRepository.existsByEventIdAndUserIdAndStatus(event.getId(), userId, EntryStatus.ACTIVE)) {
            throw alreadyEntered();
        }

        Entry entry = new Entry();
        entry.setEventId(event.getId());
        entry.setUserId(userId);
        entry.setTeamId(teamId);
        try {
            // 동시에 온 요청은 위 검사를 둘 다 통과할 수 있음 → DB 부분 유니크가 막고, 409로 바꿈
            entryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            throw alreadyEntered();
        }

        List<Ballot> ballots = new ArrayList<>();
        for (Allocation a : allocations) {
            for (int i = 0; i < a.ballots(); i++) {
                ballots.add(new Ballot(entry.getId(), a.sectionId()));
            }
        }
        ballotRepository.saveAll(ballots);
        return entry;
    }

    /** 행을 지우지 않고 상태만 바꿈 */
    public void cancel(Entry entry) {
        entry.setStatus(EntryStatus.CANCELLED);
        entry.setCancelledAt(OffsetDateTime.now());
    }

    public static ApiException alreadyEntered() {
        return new ApiException(HttpStatus.CONFLICT, "ALREADY_ENTERED", "이미 응모한 공연입니다");
    }

    // ===================== 내부 도우미 =====================

    private void validateAllocations(Event event, List<Allocation> allocations) {
        Set<UUID> sectionIds = sectionNames(event.getId()).keySet();
        Set<UUID> seen = new HashSet<>();
        int total = 0;
        for (Allocation a : allocations) {
            if (!sectionIds.contains(a.sectionId())) {
                throw badAllocation("이 공연의 구역이 아닙니다");
            }
            if (!seen.add(a.sectionId())) {
                throw badAllocation("같은 구역을 두 번 적었습니다");
            }
            if (a.ballots() < 1) {
                throw badAllocation("구역마다 제비는 1장 이상이어야 합니다");
            }
            total += a.ballots();
        }
        if (total > event.getBallotsPerUser()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_BALLOTS",
                    "제비는 최대 " + event.getBallotsPerUser() + "장까지 넣을 수 있습니다");
        }
    }

    private Entry findMyActive(Jwt jwt, UUID eventId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return entryRepository.findByEventIdAndUserIdAndStatus(eventId, userId, EntryStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", "아직 응모하지 않았습니다"));
    }

    private MyEntryResponse toMyResponse(Entry e) {
        List<Ballot> bs = ballotRepository.findByEntryId(e.getId());
        Map<UUID, String> names = sectionNames(e.getEventId());
        Map<UUID, Long> counts = bs.stream()
                .collect(Collectors.groupingBy(Ballot::getSectionId, Collectors.counting()));
        List<AllocationView> views = counts.entrySet().stream()
                .map(c -> new AllocationView(c.getKey(), names.get(c.getKey()), c.getValue().intValue()))
                .sorted(Comparator.comparing(AllocationView::sectionName))
                .toList();
        return new MyEntryResponse(e.getId(), e.getEventId(), e.getStatus(), e.getTeamId(),
                e.getCreatedAt(), bs.size(), views);
    }

    private Map<UUID, String> sectionNames(UUID eventId) {
        return sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .collect(Collectors.toMap(Section::getId, Section::getName));
    }

    /** 제비 목록 → "A 2, B 1" */
    private static String summarize(List<Ballot> bs, Map<UUID, String> names) {
        return bs.stream()
                .collect(Collectors.groupingBy(b -> names.getOrDefault(b.getSectionId(), "?"),
                        TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(x -> x.getKey() + " " + x.getValue())
                .collect(Collectors.joining(", "));
    }

    /** 821012345678 → ***-****-5678 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "***-****-" + phone.substring(phone.length() - 4);
    }

    private static ApiException badAllocation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ALLOCATION", message);
    }
}