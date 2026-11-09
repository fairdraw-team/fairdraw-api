package com.fairdraw.api.entry;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entries")
@Getter
@Setter
@NoArgsConstructor
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private UUID userId;
    private UUID teamId;                       // 개인 응모면 null
    private String status = EntryStatus.ACTIVE;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime cancelledAt;
}