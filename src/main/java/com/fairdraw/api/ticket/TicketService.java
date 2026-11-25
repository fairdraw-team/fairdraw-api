package com.fairdraw.api.ticket;

import com.fairdraw.api.payment.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

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
}