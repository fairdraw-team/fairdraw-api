package com.fairdraw.api.event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID organizerId;
    private String title;
    private String venue;
    private OffsetDateTime startsAt;
    private OffsetDateTime entryOpensAt;
    private OffsetDateTime entryClosesAt;
    private OffsetDateTime paymentDueAt;
    private Integer price;
    private Integer ballotsPerUser;
    private Integer maxTeamSize;
    private String status = "draft";
}