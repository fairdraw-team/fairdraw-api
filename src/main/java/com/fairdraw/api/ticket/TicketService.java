package com.fairdraw.api.ticket;

import static java.util.stream.Collectors.toMap;

import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventRepository;
import com.fairdraw.api.event.Section;
import com.fairdraw.api.event.SectionRepository;
import com.fairdraw.api.payment.Booking;
import com.fairdraw.api.payment.BookingRepository;
import com.fairdraw.api.payment.BookingStatus;
import com.fairdraw.api.ticket.TicketDtos.TicketResponse;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;

    /** 예매 하나에 티켓 하나. 이미 있으면 그걸 돌려줌(두 번 불러도 안전) */
    public Ticket issue(Booking booking) {
        return ticketRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            Ticket t = new Ticket();
            t.setBookingId(booking.getId());
            t.setCode(Totp.newSecretHex());
            t.setStatus(TicketStatus.VALID);
            return ticketRepository.save(t);
        });
    }

    /** 내 티켓: 내 paid 예매의 티켓을 공연 시작 순으로. code는 여기서만 나감 */
    @Transactional(readOnly = true)
    public List<TicketResponse> myTickets(Jwt jwt) {
        UUID me = UUID.fromString(jwt.getSubject());
        List<Booking> bookings = bookingRepository.findByUserIdAndStatus(me, BookingStatus.PAID);
        if (bookings.isEmpty()) return List.of();

        Map<UUID, Booking> bookingById = bookings.stream().collect(toMap(Booking::getId, b -> b));
        Map<UUID, Event> events = eventRepository
                .findAllById(bookings.stream().map(Booking::getEventId).distinct().toList()).stream()
                .collect(toMap(Event::getId, e -> e));
        Map<UUID, String> sectionNames = sectionRepository
                .findAllById(bookings.stream().map(Booking::getSectionId).distinct().toList()).stream()
                .collect(toMap(Section::getId, Section::getName));

        return ticketRepository.findByBookingIdIn(bookingById.keySet()).stream()
                .map(t -> {
                    Booking b = bookingById.get(t.getBookingId());
                    Event e = events.get(b.getEventId());
                    return new TicketResponse(t.getId(), b.getEventId(),
                            e != null ? e.getTitle() : null,
                            sectionNames.get(b.getSectionId()),
                            e != null ? e.getStartsAt() : null,
                            t.getCode(), t.getStatus(), t.getUsedAt());
                })
                .sorted(Comparator.comparing(TicketResponse::startsAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}