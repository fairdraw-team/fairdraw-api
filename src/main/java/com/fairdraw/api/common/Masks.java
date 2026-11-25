package com.fairdraw.api.common;

/** 개인정보 가리기 */
public final class Masks {

    private Masks() {}

    /** 821012345678 → ***-****-5678 */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "***-****-" + phone.substring(phone.length() - 4);
    }
}