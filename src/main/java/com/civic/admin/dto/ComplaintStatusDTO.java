package com.civic.admin.dto;

import com.civic.complaint.Complaint;

/** Admin request to change a complaint's status (with remarks shown to the citizen). */
public record ComplaintStatusDTO(Complaint.Status status, String remarks) {
}
