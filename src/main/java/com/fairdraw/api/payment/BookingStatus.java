package com.fairdraw.api.payment;

public final class BookingStatus {
    private BookingStatus() {}
    public static final String PENDING = "pending";
    public static final String PAID = "paid";
    public static final String EXPIRED = "expired";
    public static final String CANCELLED = "cancelled";
}