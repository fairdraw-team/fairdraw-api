package com.fairdraw.api.draw;

import static org.junit.jupiter.api.Assertions.*;

import com.fairdraw.api.draw.DrawEngine.DrawBallot;
import com.fairdraw.api.draw.DrawEngine.Outcome;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DrawEngineTest {

    private static UUID id(int n) { return new UUID(0, n); }

    private static Map<UUID, Outcome> byEntry(List<Outcome> out) {
        return out.stream().collect(Collectors.toMap(Outcome::entryId, Function.identity()));
    }

    // 사람·구역 id
    static final UUID MINJI = id(1), JUNHO = id(2), SEOYEON = id(3), JIHUN = id(4), HAEUN = id(5);
    static final UUID A = id(100), B = id(200);

    @Test
    void 설명서_확인용_예시_그대로() {
        // A 정원 2, B 정원 1 → 대기는 구역마다 1명
        List<DrawBallot> ordered = List.of(
                new DrawBallot(id(1001), MINJI, A),
                new DrawBallot(id(1002), JUNHO, A),
                new DrawBallot(id(1003), MINJI, B),
                new DrawBallot(id(1004), SEOYEON, A),
                new DrawBallot(id(1005), SEOYEON, B),
                new DrawBallot(id(1006), JIHUN, A),
                new DrawBallot(id(1007), HAEUN, B),
                new DrawBallot(id(1008), JIHUN, B));
        Map<UUID, Outcome> r = byEntry(DrawEngine.run(ordered, Map.of(A, 2, B, 1), Map.of()));

        assertEquals(DrawResultType.WON, r.get(MINJI).result());
        assertEquals(A, r.get(MINJI).sectionId());
        assertEquals(1, r.get(MINJI).drawOrder());

        assertEquals(DrawResultType.WON, r.get(JUNHO).result());
        assertEquals(A, r.get(JUNHO).sectionId());

        assertEquals(DrawResultType.WON, r.get(SEOYEON).result());
        assertEquals(B, r.get(SEOYEON).sectionId());
        assertEquals(id(1005), r.get(SEOYEON).ballotId());

        assertEquals(DrawResultType.WAITLISTED, r.get(JIHUN).result());
        assertEquals(A, r.get(JIHUN).sectionId());
        assertEquals(1, r.get(JIHUN).waitlistRank());

        assertEquals(DrawResultType.WAITLISTED, r.get(HAEUN).result());
        assertEquals(B, r.get(HAEUN).sectionId());
        assertEquals(1, r.get(HAEUN).waitlistRank());
    }

    @Test
    void 팀은_한_묶음으로_당첨되고_결정_제비가_같다() {
        UUID t1 = id(11), t2 = id(12), solo = id(13);
        List<UUID> team = List.of(t1, t2);
        List<DrawBallot> ordered = List.of(
                new DrawBallot(id(2001), t2, A),     // 팀원 t2의 제비가 먼저 나옴
                new DrawBallot(id(2002), solo, A),
                new DrawBallot(id(2003), t1, A));
        Map<UUID, Outcome> r = byEntry(DrawEngine.run(ordered, Map.of(A, 3), Map.of(t1, team, t2, team)));

        assertEquals(DrawResultType.WON, r.get(t1).result());
        assertEquals(DrawResultType.WON, r.get(t2).result());
        assertEquals(id(2001), r.get(t1).ballotId());   // 팀원 모두 같은 결정 제비
        assertEquals(1, r.get(t1).drawOrder());
        assertEquals(DrawResultType.WON, r.get(solo).result());
    }

    @Test
    void 남은_자리가_팀_인원보다_적으면_그_제비는_버리고_작은_구역에선_대기도_못한다() {
        UUID solo1 = id(21), t1 = id(22), t2 = id(23), solo2 = id(24);
        List<UUID> team = List.of(t1, t2);
        List<DrawBallot> ordered = List.of(
                new DrawBallot(id(3001), solo1, A),
                new DrawBallot(id(3002), t1, A),      // 남은 자리 1 < 팀 2명 → 버림
                new DrawBallot(id(3003), solo2, A),   // 남은 자리 1 → 당첨
                new DrawBallot(id(3004), t2, A));
        Map<UUID, Outcome> r = byEntry(DrawEngine.run(ordered, Map.of(A, 2), Map.of(t1, team, t2, team)));

        assertEquals(DrawResultType.WON, r.get(solo1).result());
        assertEquals(DrawResultType.WON, r.get(solo2).result());
        // 정원 2 → 대기 정원 1명인데 팀은 2명이라 대기도 안 됨
        assertEquals(DrawResultType.LOST, r.get(t1).result());
        assertEquals(DrawResultType.LOST, r.get(t2).result());
        assertNull(r.get(t1).sectionId());
        assertNull(r.get(t1).ballotId());
    }

    @Test
    void 대기는_정원의_20퍼센트까지_순번대로() {
        List<DrawBallot> ordered = new ArrayList<>();
        for (int n = 1; n <= 13; n++) {
            ordered.add(new DrawBallot(id(4000 + n), id(n), A));
        }
        // 정원 10 → 당첨 10명, 대기 ceil(10×0.2)=2명, 나머지 낙첨
        Map<UUID, Outcome> r = byEntry(DrawEngine.run(ordered, Map.of(A, 10), Map.of()));

        for (int n = 1; n <= 10; n++) assertEquals(DrawResultType.WON, r.get(id(n)).result());
        assertEquals(DrawResultType.WAITLISTED, r.get(id(11)).result());
        assertEquals(1, r.get(id(11)).waitlistRank());
        assertEquals(2, r.get(id(12)).waitlistRank());
        assertEquals(DrawResultType.LOST, r.get(id(13)).result());

        assertEquals(1, DrawEngine.waitlistLimit(5));
        assertEquals(3, DrawEngine.waitlistLimit(15));   // 소수 오차가 있으면 4가 나옴
    }

    @Test
    void 시드와_순서는_다시_계산해도_같다() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                DrawEngine.sha256Hex("abc"));

        String seed = DrawEngine.seedOf("e3b0c442", 1795154400123L);
        assertEquals(DrawEngine.sha256Hex("e3b0c442:1795154400123"), seed);

        List<String> ids = List.of("a", "b", "c", "d", "e");
        List<String> first = DrawEngine.orderOf(seed, ids);
        List<String> again = DrawEngine.orderOf(seed, List.of("e", "d", "c", "b", "a")); // 넣는 순서가 달라도
        assertEquals(first, again);
        assertEquals(new HashSet<>(ids), new HashSet<>(first));                    // 빠지거나 겹치지 않음
    }
}