package com.fairdraw.api.payment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 만료된 자리를 같은 구역 대기자에게 짝지어 주는 순수 함수 (DB·시계 안 씀) */
public final class SuccessionEngine {

    private SuccessionEngine() {}

    /** 만료된 예매 한 건 */
    public record Expired(UUID bookingId, UUID sectionId) {}

    /** 넘기기 한 건: 이 예매 자리를 → 이 응모에게 */
    public record Pair(UUID fromBookingId, UUID toEntryId) {}

    /**
     * @param expired   만료된 예매 목록 (결제 기한이 빠른 순)
     * @param waitlists 구역별 대기자 (sectionId → entryId 목록, 대기 순번이 빠른 순,
     *                  이미 예매를 받은 적 있는 응모는 뺀 목록)
     * @return 짝 목록. 그 구역에 대기자가 없으면 그 자리는 짝 없이 빠짐
     */
    public static List<Pair> pick(List<Expired> expired, Map<UUID, List<UUID>> waitlists) {
        // 받은 목록을 건드리지 않으려고 복사해서 앞에서부터 꺼냄
        Map<UUID, Deque<UUID>> queues = new HashMap<>();
        waitlists.forEach((sectionId, entries) -> queues.put(sectionId, new ArrayDeque<>(entries)));

        List<Pair> pairs = new ArrayList<>();
        for (Expired e : expired) {
            Deque<UUID> q = queues.get(e.sectionId());
            if (q == null || q.isEmpty()) continue;      // 대기자 없음 → 빈자리
            pairs.add(new Pair(e.bookingId(), q.pollFirst()));
        }
        return pairs;
    }
}