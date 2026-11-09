package com.fairdraw.api.entry;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ballots")
@Getter
@Setter
@NoArgsConstructor
public class Ballot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID entryId;
    private UUID sectionId;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Ballot(UUID entryId, UUID sectionId) {
        this.entryId = entryId;
        this.sectionId = sectionId;
    }
}