package com.fairdraw.api.event;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.common.Phones;
import com.fairdraw.api.event.EventDtos.*;
import com.fairdraw.api.profile.Profile;
import com.fairdraw.api.profile.ProfileRepository;
import com.fairdraw.api.profile.ProfileService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final EventStaffRepository eventStaffRepository;
    private final ProfileService profileService;
    private final ProfileRepository profileRepository;

    @Transactional
    public EventResponse create(Jwt jwt, CreateEventRequest req) {
        Profile me = profileService.syncFromToken(jwt); // 프로필이 있어야 organizer_id 연결 가능
        checkDateOrder(req.entryOpensAt(), req.entryClosesAt(), req.paymentDueAt(), req.startsAt());

        Event e = new Event();
        e.setOrganizerId(me.getId());
        e.setTitle(req.title().trim());
        e.setVenue(req.venue().trim());
        e.setStartsAt(req.startsAt());
        e.setEntryOpensAt(req.entryOpensAt());
        e.setEntryClosesAt(req.entryClosesAt());
        e.setPaymentDueAt(req.paymentDueAt());
        e.setPrice(req.price());
        e.setBallotsPerUser(req.ballotsPerUser() != null ? req.ballotsPerUser() : 3);
        e.setMaxTeamSize(req.maxTeamSize() != null ? req.maxTeamSize() : 2);
        eventRepository.save(e);
        return EventResponse.from(e, List.of());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> myEvents(Jwt jwt) {
        return eventRepository.findByOrganizerIdOrderByStartsAtAsc(userId(jwt)).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> openEvents() {
        return eventRepository.findByStatusOrderByStartsAtAsc("open").stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Jwt jwt, UUID id) {
        Event e = find(id);
        if ("draft".equals(e.getStatus()) && !isOwner(e, jwt)) {
            throw ApiException.notFound("공연"); // 준비 중인 공연은 주최자에게만 보임
        }
        return toResponse(e);
    }

    @Transactional
    public EventResponse update(Jwt jwt, UUID id, UpdateEventRequest req) {
        Event e = findOwned(jwt, id);

        if (hasFieldChanges(req)) {
            if (!"draft".equals(e.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "NOT_EDITABLE", "준비 중인 공연만 수정할 수 있습니다");
            }
            if (req.title() != null)
                e.setTitle(req.title().trim());
            if (req.venue() != null)
                e.setVenue(req.venue().trim());
            if (req.startsAt() != null)
                e.setStartsAt(req.startsAt());
            if (req.entryOpensAt() != null)
                e.setEntryOpensAt(req.entryOpensAt());
            if (req.entryClosesAt() != null)
                e.setEntryClosesAt(req.entryClosesAt());
            if (req.paymentDueAt() != null)
                e.setPaymentDueAt(req.paymentDueAt());
            if (req.price() != null)
                e.setPrice(req.price());
            if (req.ballotsPerUser() != null)
                e.setBallotsPerUser(req.ballotsPerUser());
            if (req.maxTeamSize() != null)
                e.setMaxTeamSize(req.maxTeamSize());
            checkDateOrder(e.getEntryOpensAt(), e.getEntryClosesAt(), e.getPaymentDueAt(), e.getStartsAt());
        }

        if (req.status() != null && !req.status().equals(e.getStatus())) {
            if (!"open".equals(req.status())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSITION",
                        "이 주소로는 응모 열기(open)만 할 수 있습니다. 마감은 마감 버튼을 쓰세요");
            }
            if (sectionRepository.findByEventIdOrderByNameAsc(id).isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NO_SECTIONS",
                        "구역을 1개 이상 만들어야 응모를 열 수 있습니다");
            }
            e.transitionTo("open");
        }
        return toResponse(e);
    }

    @Transactional
    public SectionResponse addSection(Jwt jwt, UUID eventId, CreateSectionRequest req) {
        Event e = findOwned(jwt, eventId);
        if (!"draft".equals(e.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_EDITABLE", "준비 중인 공연에만 구역을 추가할 수 있습니다");
        }
        String name = req.name().trim();
        if (sectionRepository.existsByEventIdAndName(eventId, name)) {
            throw new ApiException(HttpStatus.CONFLICT, "SECTION_NAME_TAKEN", "이미 있는 구역 이름입니다");
        }
        Section s = new Section();
        s.setEventId(eventId);
        s.setName(name);
        s.setCapacity(req.capacity());
        s.setPrice(req.price());
        sectionRepository.save(s);
        return SectionResponse.from(s);
    }

    @Transactional
    public StaffResponse addStaff(Jwt jwt, UUID eventId, AddStaffRequest req) {
        findOwned(jwt, eventId);
        String phone = Phones.normalize(req.phone());
        if (eventStaffRepository.existsByEventIdAndPhone(eventId, phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "STAFF_EXISTS", "이미 등록된 스태프입니다");
        }
        EventStaff staff = new EventStaff();
        staff.setEventId(eventId);
        staff.setPhone(phone);
        profileRepository.findByPhone(phone).ifPresent(p -> staff.setProfileId(p.getId()));
        eventStaffRepository.save(staff);
        return new StaffResponse(staff.getId(), phone, staff.getProfileId() != null);
    }

    // ---------- 아래는 위 메서드들이 같이 쓰는 도우미 ----------

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public Event find(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> ApiException.notFound("공연"));
    }

    private boolean isOwner(Event e, Jwt jwt) {
        return jwt != null && e.getOrganizerId().equals(userId(jwt));
    }

    public Event findOwned(Jwt jwt, UUID id) {
        Event e = find(id);
        if (!isOwner(e, jwt)) {
            throw ApiException.forbidden();
        }
        return e;
    }

    private EventResponse toResponse(Event e) {
        return EventResponse.from(e, sectionRepository.findByEventIdOrderByNameAsc(e.getId()));
    }

    private boolean hasFieldChanges(UpdateEventRequest r) {
        return r.title() != null || r.venue() != null || r.startsAt() != null
                || r.entryOpensAt() != null || r.entryClosesAt() != null || r.paymentDueAt() != null
                || r.price() != null || r.ballotsPerUser() != null || r.maxTeamSize() != null;
    }

    private void checkDateOrder(OffsetDateTime opens, OffsetDateTime closes,
            OffsetDateTime due, OffsetDateTime starts) {
        if (!(opens.isBefore(closes) && closes.isBefore(due) && due.isBefore(starts))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATES",
                    "날짜 순서는 응모 시작, 응모 마감, 결제 마감, 공연 일시 순이어야 합니다");
        }
    }

    /** 로그인 없이 보는 공연 상세 (준비 중인 공연은 숨김) */
    @Transactional(readOnly = true)
    public EventResponse getPublic(UUID id) {
        Event e = find(id);
        if ("draft".equals(e.getStatus())) {
            throw ApiException.notFound("공연");
        }
        return toResponse(e);
    }

    /** 응모·취소·팀 만들기·합류·탈퇴가 전부 이것 하나로 기간을 검사 */
    public void assertEntryWindowOpen(Event e) {
        if (!"open".equals(e.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_OPEN", "응모 중인 공연이 아닙니다");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(e.getEntryOpensAt()) || !now.isBefore(e.getEntryClosesAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENTRY_WINDOW_CLOSED", "응모 기간이 아닙니다");
        }
    }
}