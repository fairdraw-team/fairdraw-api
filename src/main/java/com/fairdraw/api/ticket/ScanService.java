package com.fairdraw.api.ticket;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.event.EventStaff;
import com.fairdraw.api.event.EventStaffRepository;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.payment.Booking;
import com.fairdraw.api.payment.BookingRepository;
import com.fairdraw.api.profile.Profile;
import com.fairdraw.api.profile.ProfileRepository;
import com.fairdraw.api.ticket.TicketDtos.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final EventService eventService;
    private final EventStaffRepository eventStaffRepository;
    private final ProfileRepository profileRepository;
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final SectionRepository sectionRepository;
    private final ScanLogRepository scanLogRepository;

    /** 스캔하는 사람: profileId는 나, staffId는 스태프면 event_staff 행 id(주최자면 null) */
    record Scanner(UUID profileId, UUID staffId) {}

    // ===================== 스캔 =====================

    @Transactional
    public ScanResponse scan(Jwt jwt, ScanRequest req) {
        Event event = eventService.find(req.eventId());
        Scanner sc = requireStaff(jwt, event);                          // ① 권한 (없으면 403)
        OffsetDateTime now = OffsetDateTime.now();

        // ② QR 모양: FD1.<티켓id>.<6자리>
        String code = req.code() == null ? "" : req.code().trim();
        String[] parts = code.split("\\.");
        if (parts.length != 3 || !"FD1".equals(parts[0])) {
            return invalid(event, null, sc, now, "우리 티켓의 QR이 아닙니다");
        }
        UUID ticketId;
        try {
            ticketId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            return invalid(event, null, sc, now, "우리 티켓의 QR이 아닙니다");
        }

        // ③ 이 공연의 살아 있는 티켓인가
        Ticket t = ticketRepository.findByIdForUpdate(ticketId).orElse(null);
        Booking b = (t == null) ? null : bookingRepository.findById(t.getBookingId()).orElse(null);
        if (t == null || b == null || !event.getId().equals(b.getEventId())
                || TicketStatus.VOID.equals(t.getStatus())) {
            return invalid(event, t == null ? null : t.getId(), sc, now, "이 공연의 티켓이 아닙니다");
        }
        if (!"paying".equals(event.getStatus()) && !"done".equals(event.getStatus())) {
            return invalid(event, t.getId(), sc, now, "입장 기간이 아닌 공연입니다");
        }

        // ④ 6자리 숫자가 지금(앞뒤 30초) 것과 맞는가
        if (!Totp.matches(t.getCode(), parts[2], now.toEpochSecond())) {
            return invalid(event, t.getId(), sc, now, "시간이 지난 QR입니다. 티켓 화면을 직접 보여 달라고 하세요");
        }

        String sectionName = sectionRepository.findById(b.getSectionId()).map(Section::getName).orElse(null);
        String holderName = maskName(profileRepository.findById(b.getUserId())
                .map(Profile::getRealName).orElse(null));

        // ⑤ 이미 입장했으면 duplicate (첫 입장 시각을 보여 줌)
        if (TicketStatus.USED.equals(t.getStatus())) {
            log(event, t.getId(), sc, now, ScanResult.DUPLICATE);
            return new ScanResponse(ScanResult.DUPLICATE, sectionName, t.getUsedAt(),
                    holderName, "이미 입장한 티켓입니다");
        }

        // ⑥ 입장 처리
        t.setStatus(TicketStatus.USED);
        t.setUsedAt(now);
        log(event, t.getId(), sc, now, ScanResult.OK);
        return new ScanResponse(ScanResult.OK, sectionName, now, holderName, "입장 확인");
    }

    // ===================== 스캔 요약 =====================

    @Transactional(readOnly = true)
    public ScanSummaryResponse summary(Jwt jwt, UUID eventId) {
        Event event = eventService.find(eventId);
        requireStaff(jwt, event);
        List<Booking> bookings = bookingRepository.findByEventId(eventId);
        List<Ticket> tickets = bookings.isEmpty() ? List.of()
                : ticketRepository.findByBookingIdIn(bookings.stream().map(Booking::getId).toList());
        long issued = tickets.stream().filter(t -> !TicketStatus.VOID.equals(t.getStatus())).count();
        long entered = tickets.stream().filter(t -> TicketStatus.USED.equals(t.getStatus())).count();
        return new ScanSummaryResponse(eventId, event.getTitle(), issued, entered);
    }

    // ===================== 도우미 =====================

    /** 주최자면 통과. 아니면 event_staff에서 profile_id 또는 전화번호로 찾고, 없으면 403 */
    Scanner requireStaff(Jwt jwt, Event event) {
        UUID me = UUID.fromString(jwt.getSubject());
        if (me.equals(event.getOrganizerId())) {
            return new Scanner(me, null);
        }
        Optional<EventStaff> staff = eventStaffRepository.findFirstByEventIdAndProfileId(event.getId(), me);
        if (staff.isEmpty()) {
            String phone = profileRepository.findById(me).map(Profile::getPhone).orElse(null);
            if (phone != null) {
                staff = eventStaffRepository.findFirstByEventIdAndPhone(event.getId(), phone);
            }
        }
        return staff.map(s -> new Scanner(me, s.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_STAFF", "이 공연의 스태프가 아닙니다"));
    }

    private ScanResponse invalid(Event event, UUID ticketId, Scanner sc, OffsetDateTime now, String message) {
        log(event, ticketId, sc, now, ScanResult.INVALID);
        return new ScanResponse(ScanResult.INVALID, null, now, null, message);
    }

    private void log(Event event, UUID ticketId, Scanner sc, OffsetDateTime now, String result) {
        ScanLog l = new ScanLog();
        l.setEventId(event.getId());
        l.setTicketId(ticketId);
        l.setStaffId(sc.staffId());
        l.setScannedBy(sc.profileId());
        l.setResult(result);
        l.setScannedAt(now);
        scanLogRepository.save(l);
    }

    /** 홍길동 → 홍*동, 홍길 → 홍*, 홍 → 홍 */
    static String maskName(String name) {
        if (name == null || name.isBlank()) return null;
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}