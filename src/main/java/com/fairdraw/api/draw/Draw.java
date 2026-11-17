package com.fairdraw.api.draw;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "draws")
@Getter
@Setter
@NoArgsConstructor
public class Draw {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private String rosterHash;
    private Integer ballotCount;
    private OffsetDateTime sealedAt;
    private String status = "sealed";
    private String seed;               // 추첨 시드 (봉인만 했으면 null)
    private OffsetDateTime drawnAt;    // 추첨 시각 (봉인만 했으면 null)
}