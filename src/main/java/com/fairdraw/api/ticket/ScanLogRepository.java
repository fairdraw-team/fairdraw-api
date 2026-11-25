package com.fairdraw.api.ticket;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanLogRepository extends JpaRepository<ScanLog, UUID> {
}