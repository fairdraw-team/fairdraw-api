package com.fairdraw.api.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 결제 관련 설정 값 */
@Component
public class PaymentProps {

    private final int successionPayMinutes;

    public PaymentProps(@Value("${app.succession-pay-minutes:1440}") int successionPayMinutes) {
        this.successionPayMinutes = successionPayMinutes;
    }

    /** 넘겨받은 사람의 결제 기한(분). 테스트·발표 기간은 10 */
    public int successionPayMinutes() {
        return successionPayMinutes;
    }
}