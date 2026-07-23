package com.controlleads.catalogs;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
