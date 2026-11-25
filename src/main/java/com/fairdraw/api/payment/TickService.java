package com.fairdraw.api.payment;

import com.fairdraw.api.event.Event;
import com.fairdraw.api.event.EventRepository;
import com.fairdraw.api.payment.PaymentDtos.TickResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Cron이 부르는 일괄 처리. 공연마다 따로 커밋되도록 PaymentService.process를 바깥에서 부름 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TickService {

    private final EventRepository eventRepository;
    private final PaymentService paymentService;

    public TickResponse tick() {
        OffsetDateTime now = OffsetDateTime.now();
        int events = 0, expired = 0, succeeded = 0;
        for (Event e : eventRepository.findByStatusOrderByStartsAtAsc("paying")) {
            try {
                PaymentService.ProcessResult r = paymentService.process(e.getId(), now);
                events++;
                expired += r.expired();
                succeeded += r.succeeded();
            } catch (Exception ex) {
                log.error("tick 처리 실패 eventId={}", e.getId(), ex);   // 다른 공연은 계속
            }
        }
        return new TickResponse(events, expired, succeeded);
    }
}