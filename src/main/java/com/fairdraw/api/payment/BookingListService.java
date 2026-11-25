package com.fairdraw.api.payment;

import com.fairdraw.api.common.Masks;
import com.fairdraw.api.event.EventService;
import com.fairdraw.api.payment.PaymentDtos.BookingPage;
import com.fairdraw.api.payment.PaymentDtos.BookingRow;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingListService {

    private final EventService eventService;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public BookingPage list(Jwt jwt, UUID eventId, String status, int page, int size) {
        eventService.findOwned(jwt, eventId);                       // 주최자가 아니면 403
        String filter = (status == null || status.isBlank()) ? null : status.trim();
        int safeSize = Math.min(Math.max(size, 1), 100);

        Page<Object[]> result = bookingRepository.findBookingRows(
                eventId, filter, PageRequest.of(Math.max(page, 0), safeSize));

        List<BookingRow> rows = result.getContent().stream()
                .map(r -> new BookingRow(
                        UUID.fromString((String) r[0]),
                        (String) r[1],
                        Masks.phone((String) r[2]),
                        (String) r[3],
                        (String) r[4],
                        r[5] == null ? 0 : ((Number) r[5]).intValue(),
                        toOdt(r[6]),
                        (String) r[7],
                        toOdt(r[8])))
                .toList();

        return new BookingPage(rows, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** 네이티브 쿼리의 시각은 드라이버에 따라 모양이 달라서 하나로 맞춤 */
    private static OffsetDateTime toOdt(Object o) {
        if (o == null) return null;
        if (o instanceof OffsetDateTime odt) return odt;
        if (o instanceof Instant i) return i.atOffset(ZoneOffset.UTC);
        if (o instanceof ZonedDateTime z) return z.toOffsetDateTime();
        if (o instanceof Timestamp ts) return ts.toInstant().atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("알 수 없는 시각 형식: " + o.getClass());
    }
}