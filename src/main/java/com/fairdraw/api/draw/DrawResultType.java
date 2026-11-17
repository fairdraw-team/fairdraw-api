package com.fairdraw.api.draw;

/** draw_results.result에 들어가는 글자 (DB 규칙과 글자 하나까지 같아야 함) */
public final class DrawResultType {
    public static final String WON = "won";
    public static final String WAITLISTED = "waitlisted";
    public static final String LOST = "lost";
    private DrawResultType() {}
}