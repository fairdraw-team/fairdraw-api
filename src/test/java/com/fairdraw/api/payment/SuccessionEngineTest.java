package com.fairdraw.api.payment;

import static org.junit.jupiter.api.Assertions.*;

import com.fairdraw.api.payment.SuccessionEngine.Expired;
import com.fairdraw.api.payment.SuccessionEngine.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuccessionEngineTest {

    final UUID A = UUID.randomUUID();      // A구역
    final UUID B = UUID.randomUUID();      // B구역
    final UUID junho = UUID.randomUUID();  // 만료된 예매들
    final UUID minji = UUID.randomUUID();
    final UUID seoyeon = UUID.randomUUID();
    final UUID jihun = UUID.randomUUID();  // 대기자(응모 id)
    final UUID haeun = UUID.randomUUID();

    @Test
    void 같은_구역_대기_1번에게_넘어간다() {
        List<Pair> r = SuccessionEngine.pick(
                List.of(new Expired(junho, A)),
                Map.of(A, List.of(jihun, haeun)));
        assertEquals(List.of(new Pair(junho, jihun)), r);
    }

    @Test
    void 두_자리가_만료되면_대기_순번대로_한_명씩() {
        List<Pair> r = SuccessionEngine.pick(
                List.of(new Expired(junho, A), new Expired(minji, A)),
                Map.of(A, List.of(jihun, haeun)));
        assertEquals(List.of(new Pair(junho, jihun), new Pair(minji, haeun)), r);
    }

    @Test
    void 대기자가_없으면_빈자리로_남는다() {
        List<Pair> r = SuccessionEngine.pick(
                List.of(new Expired(junho, A), new Expired(minji, A)),
                Map.of(A, List.of(jihun)));
        assertEquals(List.of(new Pair(junho, jihun)), r);   // minji 자리는 짝 없음
    }

    @Test
    void 다른_구역_대기자에게는_넘어가지_않는다() {
        List<Pair> r = SuccessionEngine.pick(
                List.of(new Expired(seoyeon, B)),
                Map.of(A, List.of(jihun)));
        assertTrue(r.isEmpty());
    }

    @Test
    void 받은_대기자_목록을_바꾸지_않는다() {
        List<UUID> waitA = new ArrayList<>(List.of(jihun, haeun));
        SuccessionEngine.pick(List.of(new Expired(junho, A)), Map.of(A, waitA));
        assertEquals(List.of(jihun, haeun), waitA);
    }
}