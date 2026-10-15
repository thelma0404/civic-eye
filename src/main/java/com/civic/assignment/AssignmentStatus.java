package com.civic.assignment;

/** ASSIGNED -> IN_PROGRESS -> COMPLETED (proof uploaded). CANCELLED when reassigned or closed by an admin. */
public enum AssignmentStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
