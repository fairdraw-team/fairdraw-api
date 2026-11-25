package com.fairdraw.api.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** 포트원 V2 REST API에 묻는 클래스. SDK 없이 HTTP로 직접 부른다 */
@Component
public class PortOneClient {

    private final String apiSecret;
    private final String storeId;
    private final String channelKey;
    private final RestClient client = RestClient.create();

    public PortOneClient(@Value("${app.portone.api-secret}") String apiSecret,
                         @Value("${app.portone.store-id}") String storeId,
                         @Value("${app.portone.channel-key}") String channelKey) {
        this.apiSecret = apiSecret;
        this.storeId = storeId;
        this.channelKey = channelKey;
    }

    /** 결제창에 넘길 값 (비밀 아님) */
    public String storeId() { return storeId; }
    public String channelKey() { return channelKey; }

    /** 포트원이 돌려주는 결제 정보 중 우리가 쓰는 것만 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PortOnePayment(String status, Amount amount) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Amount(long total) {}
    }

    /** 결제 한 건 조회. 포트원에 그 번호가 없으면(결제창만 열고 닫음) null */
    public PortOnePayment get(String paymentId) {
        try {
            return client.get()
                    .uri("https://api.portone.io/payments/{id}", paymentId)
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .body(PortOnePayment.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    /** 결제 취소 (기한이 지난 뒤 들어온 결제, 금액이 다른 결제) */
    public void cancel(String paymentId, String reason) {
        client.post()
                .uri("https://api.portone.io/payments/{id}/cancel", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", reason))
                .retrieve()
                .toBodilessEntity();
    }
}