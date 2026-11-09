package com.fairdraw.api.team;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private UUID sectionId;
    private UUID leaderId;
    private String inviteCode;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}