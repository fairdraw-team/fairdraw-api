package com.fairdraw.api.payment;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.draw.DrawResult;
import com.fairdraw.api.draw.DrawResultRepository;
import com.fairdraw.api.draw.DrawResultType;
import com.fairdraw.api.entry.Entry;
import com.fairdraw.api.entry.EntryRepository;
import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventRepository;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.payment.PaymentDtos.*;
import com.fairdraw.api.ticket.Ticket;
import com.fairdraw.api.ticket.TicketRepository;
import com.fairdraw.api.ticket.TicketService;
import com.fairdraw.api.ticket.TicketStatus;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final DrawResultRepository drawResultRepository;
    private final EntryRepository entryRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final PortOneClient portOneClient;
    private final PaymentSyncService paymentSyncService;
    private final EntityManager entityManager;

    // ===================== 결제 열기 (BE-3) =====================

    @Transactional
    public PaymentStatsResponse open(Jwt jwt, UUID eventId) {
        eventService.findOwned(jwt, eventId);                          // ① 주최자가 아니면 403
        Event event = eventRepository.findByIdForUpdate(eventId)       // ② 공연 행 잠금
                .orElseThrow(() -> ApiException.notFound("공연"));
        entityManager.refresh(event);

        String status = event.getStatus();                              // ③ 상태 검사
        if ("paying".equals(status) || "done".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PAYING", "이미 결제를 연 공연입니다");
        }
        if (!"drawn".equals(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EVENT_NOT_DRAWN", "추첨을 마친 공연만 결제를 열 수 있습니다");
        }

        OffsetDateTime now = OffsetDateTime.now();                      // ④ 결제 마감 검사
        if (event.getPaymentDueAt() == null || !now.isBefore(event.getPaymentDueAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_WINDOW_CLOSED", "결제 마감이 지났습니다");
        }

        Map<UUID, Section> sections = sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .collect(toMap(Section::getId, s -> s));
        List<DrawResult> won = drawResultRepository.findByEventIdAndResult(eventId, DrawResultType.WON);
        Map<UUID, UUID> userByEntry = entryRepository
                .findAllById(won.stream().map(DrawResult::getEntryId).toList()).stream()
                .collect(toMap(Entry::getId, Entry::getUserId));

        for (DrawResult r : won) {                                      // ⑤ 당첨자마다 예매 한 행
            Section s = sections.get(r.getSectionId());
            int amount = (s != null && s.getPrice() != null)
                    ? s.getPrice()
                    : Objects.requireNonNullElse(event.getPrice(), 0);

            Booking b = new Booking();
            b.setEventId(eventId);
            b.setUserId(userByEntry.get(r.getEntryId()));
            b.setSectionId(r.getSectionId());
            b.setDrawResultId(r.getId());
            b.setEntryId(r.getEntryId());
            b.setAmount(amount);
            b.setPayDueAt(event.getPaymentDueAt());
            b.setSource(BookingSource.DRAW);
            if (amount == 0) {                                          // ⑥ 0원 자리는 바로 paid
                b.setStatus(BookingStatus.PAID);
                b.setPaidAt(now);
            } else {
                b.setStatus(BookingStatus.PENDING);
            }
            bookingRepository.save(b);
            if (amount == 0) {
                ticketService.issue(b);
            }
        }

        event.transitionTo("paying");                                   // ⑦
        return toStats(event);
    }

    // ===================== 내 예매 (BE-4) =====================

    @Transactional(readOnly = true)
    public MyBookingResponse myBooking(Jwt jwt, UUID eventId) {
        UUID me = UUID.fromString(jwt.getSubject());
        Booking b = bookingRepository.findFirstByEventIdAndUserIdOrderByCreatedAtDesc(eventId, me)
                .orElseThrow(PaymentService::bookingNotFound);
        String sectionName = sectionRepository.findById(b.getSectionId())
                .map(Section::getName).orElse(null);
        return new MyBookingResponse(b.getId(), b.getEventId(), b.getStatus(), sectionName,
                b.getAmount(), b.getPayDueAt(), b.getSource(), b.getPaidAt());
    }

    // ===================== 결제 준비 (BE-4) =====================

    @Transactional
    public PrepareResponse prepare(Jwt jwt, PrepareRequest req) {
        UUID me = UUID.fromString(jwt.getSubject());
        Booking b = bookingRepository.findById(req.bookingId())          // ① 찾기
                .orElseThrow(PaymentService::bookingNotFound);
        if (!me.equals(b.getUserId())) {                                  //    내 것인가
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 예매가 아닙니다");
        }
        if (!BookingStatus.PENDING.equals(b.getStatus())) {               // ② 결제 대기인가
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_PENDING", "결제할 수 없는 예매입니다");
        }
        if (!OffsetDateTime.now().isBefore(b.getPayDueAt())) {            // ③ 기한 안인가
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_WINDOW_CLOSED", "결제 기한이 지났습니다");
        }

        // ④ 결제 번호: fd + 32글자 = 영문 소문자·숫자 34글자
        String paymentId = "fd" + UUID.randomUUID().toString().replace("-", "");

        Payment p = new Payment();                                        // ⑤ 누를 때마다 새 행
        p.setBookingId(b.getId());
        p.setPaymentId(paymentId);
        p.setAmount(b.getAmount());
        p.setStatus(PaymentStatus.READY);
        paymentRepository.save(p);

        Event event = eventService.find(b.getEventId());                  // ⑥ 결제창에 넘길 값
        String sectionName = sectionRepository.findById(b.getSectionId())
                .map(Section::getName).orElse("");
        String orderName = (event.getTitle() + " " + sectionName).trim();
        return new PrepareResponse(paymentId, b.getAmount(), orderName,
                portOneClient.storeId(), portOneClient.channelKey());
    }

    // ===================== 결제 확인 (BE-5) =====================

    /** 트랜잭션 없음: sync가 따로 커밋한 뒤에 에러로 바꿔야 취소 기록이 남음 */
    public CompleteResponse complete(Jwt jwt, CompleteRequest req) {
        UUID me = UUID.fromString(jwt.getSubject());
        Payment p = paymentRepository.findByPaymentId(req.paymentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 기록이 없습니다"));
        Booking b = bookingRepository.findById(p.getBookingId())
                .orElseThrow(PaymentService::bookingNotFound);
        if (!me.equals(b.getUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "내 결제가 아닙니다");
        }

        PaymentSyncService.Result r = paymentSyncService.sync(req.paymentId());
        switch (r) {
            case PAID -> {
                return new CompleteResponse(p.getPaymentId(), PaymentStatus.PAID, b.getId(), b.getEventId());
            }
            case NOT_FOUND -> throw new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 기록이 없습니다");
            case NOT_PAID -> throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_NOT_PAID", "아직 결제가 완료되지 않았습니다");
            case AMOUNT_MISMATCH -> throw new ApiException(HttpStatus.CONFLICT, "AMOUNT_MISMATCH", "결제 금액이 달라 취소했습니다");
            case BOOKING_EXPIRED -> throw new ApiException(HttpStatus.CONFLICT, "BOOKING_EXPIRED", "결제 기한이 지나 취소했습니다");
            default -> throw new IllegalStateException("알 수 없는 결과: " + r);
        }
    }

    // ===================== 결제 현황 (BE-11) =====================

    @Transactional(readOnly = true)
    public PaymentStatsResponse stats(Jwt jwt, UUID eventId) {
        return toStats(eventService.findOwned(jwt, eventId));
    }

    PaymentStatsResponse toStats(Event event) {
        UUID eventId = event.getId();
        List<Booking> bookings = bookingRepository.findByEventId(eventId);
        Set<UUID> bookedEntries = bookings.stream()
                .map(Booking::getEntryId).filter(Objects::nonNull).collect(toSet());
        List<DrawResult> waitlisted = drawResultRepository.findByEventIdAndResult(eventId, DrawResultType.WAITLISTED);
        List<Ticket> tickets = bookings.isEmpty() ? List.of()
                : ticketRepository.findByBookingIdIn(bookings.stream().map(Booking::getId).toList());

        long issued = tickets.stream().filter(t -> !TicketStatus.VOID.equals(t.getStatus())).count();
        long entered = tickets.stream().filter(t -> TicketStatus.USED.equals(t.getStatus())).count();

        List<SectionPaymentStat> rows = sectionRepository.findByEventIdOrderByNameAsc(eventId).stream()
                .map(s -> {
                    List<Booking> mine = bookings.stream().filter(b -> s.getId().equals(b.getSectionId())).toList();
                    return new SectionPaymentStat(
                            s.getId(), s.getName(), s.getCapacity(),
                            count(mine, BookingStatus.PAID),
                            count(mine, BookingStatus.PENDING),
                            count(mine, BookingStatus.EXPIRED),
                            mine.stream().filter(b -> BookingSource.SUCCESSION.equals(b.getSource())).count(),
                            waitlisted.stream()
                                    .filter(r -> s.getId().equals(r.getSectionId()))
                                    .filter(r -> !bookedEntries.contains(r.getEntryId()))
                                    .count());
                })
                .toList();

        return new PaymentStatsResponse(eventId, event.getStatus(), event.getPaymentDueAt(),
                issued, entered, rows);
    }

    // ===================== 도우미 =====================

    private static long count(List<Booking> list, String status) {
        return list.stream().filter(b -> status.equals(b.getStatus())).count();
    }

    private static ApiException bookingNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "예매가 없습니다");
    }
}