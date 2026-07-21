package com.controlleads.catalogs;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StallReasonRepository extends JpaRepository<StallReason, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
