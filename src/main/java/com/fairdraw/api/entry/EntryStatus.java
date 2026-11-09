package com.fairdraw.api.entry;

/** DB의 check 규칙과 글자 하나까지 같아야 함 (소문자) */
public final class EntryStatus {
    public static final String ACTIVE = "active";
    public static final String CANCELLED = "cancelled";
    private EntryStatus() {}
}