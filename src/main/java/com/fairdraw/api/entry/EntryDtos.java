package com.fairdraw.api.entry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class EntryDtos {

    /** 구역 하나에 제비 몇 장 */
    public record Allocation(@NotNull UUID sectionId, @NotNull @Min(1) Integer ballots) {}

    /** 응모 요청: { "allocations": [ { "sectionId": "…", "ballots": 2 } ] } */
    public record CreateEntryRequest(@NotEmpty List<@Valid Allocation> allocations) {}

    public record AllocationView(UUID sectionId, String sectionName, int ballots) {}

    /** 내 응모 */
    public record MyEntryResponse(UUID id, UUID eventId, String status, UUID teamId,
                                  OffsetDateTime createdAt, int totalBallots,
                                  List<AllocationView> allocations) {}

    /** 주최자용 명단 한 줄 (전화번호는 뒤 4자리만) */
    public record EntryRow(UUID id, String realName, String phone, String allocations,
                           int ballots, String status, UUID teamId, OffsetDateTime createdAt) {}

    public record EntryPage(List<EntryRow> content, int page, int size,
                            long totalElements, int totalPages) {}
}