package com.fairdraw.api.draw;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** 추첨 계산. DB와 시계를 쓰지 않는 순수 함수만 둠 */
public final class DrawEngine {

    private DrawEngine() {}

    /** 입력: 추첨 순서대로 줄 선 제비 한 장 */
    public record DrawBallot(UUID ballotId, UUID entryId, UUID sectionId) {}

    /** 출력: 응모 하나의 결과. 낙첨이면 sectionId·waitlistRank·ballotId·drawOrder가 null */
    public record Outcome(UUID entryId, String result, UUID sectionId,
                          Integer waitlistRank, UUID ballotId, Integer drawOrder) {}

    // ===================== BE-3: 해시·시드·순서 =====================

    /** 글자 → SHA-256 → 소문자 16진수 64글자 */
    public static String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // 람다 안에서도 쓸 수 있게 바꿔 던짐
        }
    }

    /** 시드 = sha256Hex(봉인 해시 + ":" + 추첨 시각 밀리초) */
    public static String seedOf(String rosterHash, long drawnAtMillis) {
        return sha256Hex(rosterHash + ":" + drawnAtMillis);
    }

    /** 제비마다 sha256Hex(seed + ":" + 제비 id)를 구해 그 값의 오름차순으로 줄 세움 */
    public static List<String> orderOf(String seed, List<String> ballotIds) {
        return ballotIds.stream()
                .sorted(Comparator.comparing((String id) -> sha256Hex(seed + ":" + id)))
                .toList();
    }

    // ===================== BE-4: 판정 =====================

    /** 구역 대기 정원 = ceil(정원 × 0.2). 소수 계산 오차를 피하려고 정수로 계산 */
    public static int waitlistLimit(int capacity) {
        return (capacity + 4) / 5;
    }

    /**
     * 판정.
     * @param ordered  추첨 순서대로 줄 선 제비
     * @param capacity 구역별 정원
     * @param groups   응모별 묶음 (팀이면 팀원 전원의 entryId, 개인이면 없어도 됨)
     */
    public static List<Outcome> run(List<DrawBallot> ordered,
                                    Map<UUID, Integer> capacity,
                                    Map<UUID, List<UUID>> groups) {
        Map<UUID, Outcome> decided = new HashMap<>();

        // ---- 첫 바퀴: 당첨 ----
        Map<UUID, Integer> wonCount = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            DrawBallot b = ordered.get(i);
            if (decided.containsKey(b.entryId())) continue;          // 한 사람 한 자리: 이미 당첨이면 건너뜀
            List<UUID> group = groupOf(groups, b.entryId());
            int left = capacity.getOrDefault(b.sectionId(), 0) - wonCount.getOrDefault(b.sectionId(), 0);
            if (left < group.size()) continue;                        // 자리 부족: 이 제비만 버림
            for (UUID member : group) {
                decided.put(member, new Outcome(member, DrawResultType.WON, b.sectionId(),
                        null, b.ballotId(), i + 1));
            }
            wonCount.merge(b.sectionId(), group.size(), Integer::sum);
        }

        // ---- 둘째 바퀴: 대기 (같은 줄을 처음부터 다시) ----
        Map<UUID, Integer> waitCount = new HashMap<>();   // 구역별 대기 사람 수
        Map<UUID, Integer> waitRank = new HashMap<>();    // 구역별 마지막으로 준 대기 번호
        for (int i = 0; i < ordered.size(); i++) {
            DrawBallot b = ordered.get(i);
            if (decided.containsKey(b.entryId())) continue;          // 당첨자·이미 대기 중이면 건너뜀
            List<UUID> group = groupOf(groups, b.entryId());
            int limit = waitlistLimit(capacity.getOrDefault(b.sectionId(), 0));
            if (waitCount.getOrDefault(b.sectionId(), 0) + group.size() > limit) continue;
            int rank = waitRank.merge(b.sectionId(), 1, Integer::sum); // 묶음마다 번호 하나 (팀원은 같은 번호)
            for (UUID member : group) {
                decided.put(member, new Outcome(member, DrawResultType.WAITLISTED, b.sectionId(),
                        rank, b.ballotId(), i + 1));
            }
            waitCount.merge(b.sectionId(), group.size(), Integer::sum);
        }

        // ---- 낙첨: 나머지 전부 ----
        LinkedHashSet<UUID> everyone = new LinkedHashSet<>();
        for (DrawBallot b : ordered) everyone.addAll(groupOf(groups, b.entryId()));
        List<Outcome> out = new ArrayList<>();
        for (UUID entryId : everyone) {
            out.add(decided.getOrDefault(entryId,
                    new Outcome(entryId, DrawResultType.LOST, null, null, null, null)));
        }
        return out;
    }

    /** 묶음이 없으면 자기 하나 */
    private static List<UUID> groupOf(Map<UUID, List<UUID>> groups, UUID entryId) {
        List<UUID> g = groups.get(entryId);
        return (g == null || g.isEmpty()) ? List.of(entryId) : g;
    }
}