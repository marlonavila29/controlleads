package com.controlleads.leads;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadAssignmentEventRepository extends JpaRepository<LeadAssignmentEvent, UUID> {
    List<LeadAssignmentEvent> findByLeadIdOrderByChangedAtAsc(UUID leadId);
}
