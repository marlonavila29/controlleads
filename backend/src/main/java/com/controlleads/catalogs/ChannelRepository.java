package com.controlleads.catalogs;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
