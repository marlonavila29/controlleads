package com.controlleads.leads;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    List<Lead> findByPhoneAndDeletedAtIsNull(String phone);

    /** HOT_LEADs that have gone uncontacted past the SLA threshold. */
    @Query("""
        SELECT l FROM Lead l
        WHERE l.status = com.controlleads.leads.LeadStatus.HOT_LEAD
          AND l.deletedAt IS NULL
          AND (l.lastContactedAt IS NULL OR l.lastContactedAt < :threshold)
        """)
    List<Lead> findSlaBreaching(@Param("threshold") Instant threshold);
}
