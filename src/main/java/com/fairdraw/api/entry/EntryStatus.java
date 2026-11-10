package com.fairdraw.api.entry;

/**
 * DB의 entries_status_check 규칙과 글자 하나까지 같아야 함 (소문자).
 * 지금 DB 규칙: 'submitted', 'cancelled', 'won', 'lost'
 * (V3에서 active로 바뀌지 않아서 임시로 submitted를 씀. DB가 V4로 바뀌면 "active"로 되돌릴 것)
 */
public final class EntryStatus {
    public static final String ACTIVE = "submitted";
    public static final String CANCELLED = "cancelled";
    private EntryStatus() {}
}