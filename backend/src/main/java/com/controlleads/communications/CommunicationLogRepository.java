package com.controlleads.communications;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, UUID> {
    
    List<CommunicationLog> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
    
    @Query("SELECT c FROM CommunicationLog c JOIN FETCH c.lead JOIN FETCH c.sentBy ORDER BY c.createdAt DESC")
    List<CommunicationLog> findAllWithRelations();
}
