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
}