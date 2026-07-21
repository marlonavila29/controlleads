package com.controlleads.leads;

import com.controlleads.catalogs.ChannelRepository;
import com.controlleads.catalogs.CourseRepository;
import com.controlleads.catalogs.StallReasonRepository;
import com.controlleads.common.ApiException;
import com.controlleads.common.CurrentUser;
import com.controlleads.leads.LeadController.CreateLeadRequest;
import com.controlleads.leads.LeadController.TransitionRequest;
import com.controlleads.leads.LeadController.UpdateLeadRequest;
import com.controlleads.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {

    private final LeadRepository leads;
    private final LeadStatusEventRepository statusEvents;
    private final LeadAssignmentEventRepository assignmentEvents;
    private final CourseRepository courses;
    private final ChannelRepository channels;
    private final StallReasonRepository stallReasons;
    private final UserRepository users;

    public LeadService(LeadRepository leads, LeadStatusEventRepository statusEvents,
                       LeadAssignmentEventRepository assignmentEvents, CourseRepository courses,
                       ChannelRepository channels, StallReasonRepository stallReasons,
                       UserRepository users) {
        this.leads = leads;
        this.statusEvents = statusEvents;
        this.assignmentEvents = assignmentEvents;
        this.courses = courses;
        this.channels = channels;
        this.stallReasons = stallReasons;
        this.users = users;
    }

    /** Marketing works only on own leads; admins on all (CLAUDE.md rule 3). */
    public Lead loadVisible(UUID id, CurrentUser caller) {
        Lead lead = leads.findById(id)
            .filter(l -> l.getDeletedAt() == null)
            .orElseThrow(() -> ApiException.notFound("Lead not found"));
        if (!caller.isAdmin() && !lead.getAssignedTo().equals(caller.id())) {
            // Not-found (not 403) so members cannot probe other people's lead ids.
            throw ApiException.notFound("Lead not found");
        }
        return lead;
    }

    @Transactional
    public Lead create(CreateLeadRequest request, CurrentUser caller) {
        requireActiveCourse(request.courseId());
        requireActiveChannel(request.channelId());

        UUID owner = caller.id();
        if (request.assignedTo() != null && !request.assignedTo().equals(caller.id())) {
            if (!caller.isAdmin()) {
                throw ApiException.badRequest("Only administrators can assign leads to someone else");
            }
            requireActiveUser(request.assignedTo());
            owner = request.assignedTo();
        }

        Lead lead = new Lead(request.fullName().trim(), request.countryCode().toUpperCase(),
            normalize(request.email()), normalize(request.phone()),
            request.courseId(), request.channelId(), owner, caller.id());
        lead.setUtmSource(normalize(request.utmSource()));
        lead.setUtmMedium(normalize(request.utmMedium()));
        lead.setUtmCampaign(normalize(request.utmCampaign()));
        leads.save(lead);

        statusEvents.save(new LeadStatusEvent(lead.getId(), null, LeadStatus.LEAD, null, null, caller.id()));
        assignmentEvents.save(new LeadAssignmentEvent(lead.getId(), null, owner, caller.id()));
        return lead;
    }

    @Transactional
    public Lead update(UUID id, UpdateLeadRequest request, CurrentUser caller) {
        Lead lead = loadVisible(id, caller);
        if (request.fullName() != null) lead.setFullName(request.fullName().trim());
        if (request.countryCode() != null) lead.setCountryCode(request.countryCode().toUpperCase());
        if (request.email() != null) lead.setEmail(normalize(request.email()));
        if (request.phone() != null) lead.setPhone(normalize(request.phone()));
        if (request.courseId() != null) {
            requireActiveCourse(request.courseId());
            lead.setCourseId(request.courseId());
        }
        if (request.channelId() != null) {
            requireActiveChannel(request.channelId());
            lead.setChannelId(request.channelId());
        }
        if (request.assignedTo() != null && !request.assignedTo().equals(lead.getAssignedTo())) {
            if (!caller.isAdmin()) {
                throw ApiException.badRequest("Only administrators can reassign leads");
            }
            requireActiveUser(request.assignedTo());
            assignmentEvents.save(new LeadAssignmentEvent(
                lead.getId(), lead.getAssignedTo(), request.assignedTo(), caller.id()));
            lead.setAssignedTo(request.assignedTo());
        }
        return lead;
    }

    /**
     * Reassign every active lead of one user to another (module_auth.md RN-02),
     * e.g. when deactivating a team member. Each move is an audited assignment
     * event. Admin-only (enforced at the controller).
     */
    @Transactional
    public int reassignAll(UUID fromUserId, UUID toUserId, CurrentUser admin) {
        if (fromUserId.equals(toUserId)) {
            throw ApiException.badRequest("Pick a different user to receive the leads");
        }
        requireActiveUser(toUserId);
        List<Lead> owned = leads.findByAssignedToAndDeletedAtIsNull(fromUserId);
        for (Lead lead : owned) {
            assignmentEvents.save(new LeadAssignmentEvent(lead.getId(), fromUserId, toUserId, admin.id()));
            lead.setAssignedTo(toUserId);
        }
        return owned.size();
    }

    /**
     * Funnel transition (module_leads.md RN-01/RN-02). Every accepted move
     * writes an immutable LeadStatusEvent.
     */
    @Transactional
    public Lead transition(UUID id, TransitionRequest request, CurrentUser caller) {
        Lead lead = loadVisible(id, caller);
        LeadStatus from = lead.getStatus();
        LeadStatus to = request.toStatus();

        if (to == from) {
            throw ApiException.badRequest("Lead is already in " + to);
        }
        UUID stallReasonId = null;
        if (to == LeadStatus.STALLED) {
            if (request.stallReasonId() == null) {
                throw ApiException.badRequest("A stall reason is required to mark a lead as STALLED");
            }
            stallReasons.findById(request.stallReasonId())
                .orElseThrow(() -> ApiException.badRequest("Unknown stall reason"));
            stallReasonId = request.stallReasonId();
        }
        if (!caller.isAdmin()) {
            validateMemberTransition(lead, from, to);
        }

        lead.moveTo(to, stallReasonId);
        statusEvents.save(new LeadStatusEvent(lead.getId(), from, to, stallReasonId,
            normalize(request.note()), caller.id()));
        return lead;
    }

    private void validateMemberTransition(Lead lead, LeadStatus from, LeadStatus to) {
        if (from == LeadStatus.STUDENT) {
            throw ApiException.badRequest("STUDENT is final — only an administrator can correct it");
        }
        if (to == LeadStatus.STALLED) {
            return; // any non-terminal stage may stall
        }
        if (from == LeadStatus.STALLED) {
            if (to != lead.getStalledFromStatus()) {
                throw ApiException.badRequest(
                    "A stalled lead can only be reactivated back to " + lead.getStalledFromStatus());
            }
            return;
        }
        if (to != from.next()) {
            throw ApiException.badRequest("Invalid transition " + from + " → " + to);
        }
    }

    private void requireActiveCourse(UUID id) {
        courses.findById(id).filter(c -> c.isActive())
            .orElseThrow(() -> ApiException.badRequest("Unknown or inactive course"));
    }

    private void requireActiveChannel(UUID id) {
        channels.findById(id).filter(c -> c.isActive())
            .orElseThrow(() -> ApiException.badRequest("Unknown or inactive channel"));
    }

    private void requireActiveUser(UUID id) {
        users.findById(id).filter(u -> u.isActive())
            .orElseThrow(() -> ApiException.badRequest("Unknown or inactive user"));
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
