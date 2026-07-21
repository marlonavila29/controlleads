package com.controlleads.activities;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findByLeadIdOrderByCreatedAtAsc(UUID leadId);

    /** Open follow-ups on leads currently owned by the given user. */
    @Query("""
        SELECT a FROM Activity a
        WHERE a.type = com.controlleads.activities.ActivityType.FOLLOW_UP
          AND a.completedAt IS NULL
          AND a.leadId IN (SELECT l.id FROM Lead l
                           WHERE l.assignedTo = :userId AND l.deletedAt IS NULL)
        ORDER BY a.dueAt ASC
        """)
    List<Activity> findOpenFollowUpsForOwner(@Param("userId") UUID userId);
}
