package com.controlleads.activities;

import com.controlleads.common.ApiException;
import com.controlleads.common.CurrentUser;
import com.controlleads.leads.Lead;
import com.controlleads.leads.LeadService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activities;
    private final LeadService leadService;

    public ActivityService(ActivityRepository activities, LeadService leadService) {
        this.activities = activities;
        this.leadService = leadService;
    }

    public List<Activity> listByLead(UUID leadId, CurrentUser caller) {
        leadService.loadVisible(leadId, caller); // ownership gate
        return activities.findByLeadIdOrderByCreatedAtAsc(leadId);
    }

    @Transactional
    public Activity add(UUID leadId, ActivityType type, String content, Instant dueAt,
                        CurrentUser caller) {
        Lead lead = leadService.loadVisible(leadId, caller);
        String cleaned = content == null ? "" : content.trim();
        if (cleaned.isEmpty()) {
            throw ApiException.badRequest("Activity content is required");
        }
        if (type != ActivityType.FOLLOW_UP && dueAt != null) {
            throw ApiException.badRequest("Only follow-ups can have a due date");
        }
        if (type == ActivityType.FOLLOW_UP && dueAt == null) {
            throw ApiException.badRequest("A follow-up needs a due date");
        }
        Activity activity = activities.save(
            new Activity(leadId, type, cleaned, type == ActivityType.FOLLOW_UP ? dueAt : null, caller.id()));

        // Contact activities reset the SLA clock (module_activities.md RN-01).
        if (type.isContact()) {
            lead.setLastContactedAt(Instant.now());
        }
        return activity;
    }

    @Transactional
    public Activity complete(UUID activityId, CurrentUser caller) {
        Activity activity = activities.findById(activityId)
            .orElseThrow(() -> ApiException.notFound("Activity not found"));
        leadService.loadVisible(activity.getLeadId(), caller); // ownership gate
        if (activity.getType() != ActivityType.FOLLOW_UP) {
            throw ApiException.badRequest("Only follow-ups can be completed");
        }
        activity.complete();
        return activity;
    }

    /**
     * "My tasks" follow lead ownership, not who created them: after an admin
     * reassigns a lead, its open follow-ups move to the new owner — who is also
     * the only non-admin able to complete them (keeps this consistent with the
     * complete() ownership gate).
     */
    public List<Activity> myOpenFollowUps(CurrentUser caller) {
        return activities.findOpenFollowUpsForOwner(caller.id());
    }
}
