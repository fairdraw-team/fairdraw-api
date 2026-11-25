package com.fairdraw.api.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuccessionRepository extends JpaRepository<Succession, UUID> {
}