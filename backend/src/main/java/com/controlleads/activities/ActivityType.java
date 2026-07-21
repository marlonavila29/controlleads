package com.controlleads.activities;

/**
 * Activity kinds (module_activities.md). The contact kinds reset the SLA
 * clock by updating leads.last_contacted_at.
 */
public enum ActivityType {
    NOTE,
    CALL,
    EMAIL,
    WHATSAPP,
    MEETING,
    FOLLOW_UP;

    public boolean isContact() {
        return this == CALL || this == EMAIL || this == WHATSAPP || this == MEETING;
    }
}
