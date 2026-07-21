package com.controlleads.leads;

/**
 * The funnel. Valid transitions (module_leads.md RN-01):
 * sequential advance LEAD → HOT_LEAD → APPLICATION → STUDENT;
 * any non-terminal stage → STALLED; STALLED → the stage it left.
 * Administrators may perform any transition (recorded).
 */
public enum LeadStatus {
    LEAD,
    HOT_LEAD,
    APPLICATION,
    STUDENT,
    STALLED;

    /** Next stage in the happy path, or null when there is none. */
    public LeadStatus next() {
        return switch (this) {
            case LEAD -> HOT_LEAD;
            case HOT_LEAD -> APPLICATION;
            case APPLICATION -> STUDENT;
            case STUDENT, STALLED -> null;
        };
    }
}
