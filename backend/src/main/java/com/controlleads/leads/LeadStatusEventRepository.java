package com.controlleads.leads;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadStatusEventRepository extends JpaRepository<LeadStatusEvent, UUID> {
    List<LeadStatusEvent> findByLeadIdOrderByChangedAtAsc(UUID leadId);
}
