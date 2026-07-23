package com.controlleads.communications;

import com.controlleads.activities.ActivityService;
import com.controlleads.activities.ActivityType;
import com.controlleads.catalogs.Course;
import com.controlleads.catalogs.CourseRepository;
import com.controlleads.common.ApiException;
import com.controlleads.common.CurrentUser;
import com.controlleads.leads.Lead;
import com.controlleads.leads.LeadService;
import com.controlleads.users.User;
import com.controlleads.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationService {

    private final CommunicationLogRepository commLogs;
    private final LeadService leadService;
    private final ActivityService activityService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CommunicationService(CommunicationLogRepository commLogs, LeadService leadService,
                                ActivityService activityService, UserRepository userRepository,
                                CourseRepository courseRepository) {
        this.commLogs = commLogs;
        this.leadService = leadService;
        this.activityService = activityService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public record SendBatchRequest(List<UUID> leadIds, String channel, String subject, String body) {}
    public record SendResultDto(int total, int sent, int failed) {}

    @Transactional
    public SendResultDto sendBatch(SendBatchRequest request, CurrentUser caller) {
        User sender = userRepository.findById(caller.id())
            .orElseThrow(() -> ApiException.notFound("Sender user not found"));

        int sent = 0;
        int failed = 0;

        for (UUID leadId : request.leadIds()) {
            try {
                // Load visible lead to ensure caller has access permissions
                Lead lead = leadService.loadVisible(leadId, caller);

                // Fetch details for placeholder replacement
                String courseName = "N/A";
                if (lead.getCourseId() != null) {
                    Course course = courseRepository.findById(lead.getCourseId()).orElse(null);
                    if (course != null) {
                        courseName = course.getName();
                    }
                }

                String counselorName = "Your Counselor";
                if (lead.getAssignedTo() != null) {
                    User counselor = userRepository.findById(lead.getAssignedTo()).orElse(null);
                    if (counselor != null) {
                        counselorName = counselor.getName();
                    }
                }

                // Placeholder replacements
                String resolvedBody = request.body()
                    .replace("{name}", lead.getFullName())
                    .replace("{course}", courseName)
                    .replace("{counselor}", counselorName);

                String channel = request.channel().toUpperCase();
                String recipientAddress = "";
                boolean hasContactAddress = false;

                if ("EMAIL".equals(channel)) {
                    recipientAddress = lead.getEmail();
                    hasContactAddress = recipientAddress != null && !recipientAddress.trim().isEmpty();
                } else if ("WHATSAPP".equals(channel)) {
                    recipientAddress = lead.getPhone();
                    hasContactAddress = recipientAddress != null && !recipientAddress.trim().isEmpty();
                }

                String status = hasContactAddress ? "SENT" : "FAILED";

                // Save communication log
                CommunicationLog log = new CommunicationLog(lead, sender, channel, recipientAddress != null ? recipientAddress : "Unknown", request.subject(), resolvedBody, status);
                commLogs.save(log);

                if (hasContactAddress) {
                    // Save lead activity (resets SLA clock)
                    ActivityType actType = "EMAIL".equals(channel) ? ActivityType.EMAIL : ActivityType.WHATSAPP;
                    String activityDesc = "EMAIL".equals(channel) 
                        ? "Email sent: " + (request.subject() != null ? "[" + request.subject() + "] " : "") + resolvedBody
                        : "WhatsApp message sent: " + resolvedBody;

                    activityService.add(leadId, actType, activityDesc, null, caller);
                    sent++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }

        return new SendResultDto(request.leadIds().size(), sent, failed);
    }

    public List<CommunicationLog> getLogsForLead(UUID leadId, CurrentUser caller) {
        leadService.loadVisible(leadId, caller); // gate check
        return commLogs.findByLeadIdOrderByCreatedAtDesc(leadId);
    }

    public List<CommunicationLog> getAllLogs() {
        return commLogs.findAllWithRelations();
    }
}
