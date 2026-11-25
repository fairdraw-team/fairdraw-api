package com.fairdraw.api.payment;

public final class PaymentStatus {
    private PaymentStatus() {}
    public static final String READY = "ready";
    public static final String PAID = "paid";
    public static final String FAILED = "failed";
    public static final String CANCELLED = "cancelled";
}