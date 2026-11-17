package com.fairdraw.api.draw;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawResultRepository extends JpaRepository<DrawResult, UUID> {
    List<DrawResult> findByDrawId(UUID drawId);
    Optional<DrawResult> findByDrawIdAndEntryId(UUID drawId, UUID entryId);
    boolean existsByDrawId(UUID drawId);
}