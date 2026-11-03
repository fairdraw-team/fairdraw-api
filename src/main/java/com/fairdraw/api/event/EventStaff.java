package com.fairdraw.api.event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_staff")
@Getter
@Setter
@NoArgsConstructor
public class EventStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private String phone;      // 821012345678
    private UUID profileId;    // 가입한 사람이면 연결, 아니면 null
}