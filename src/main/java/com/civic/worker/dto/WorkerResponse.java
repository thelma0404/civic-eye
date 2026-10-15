package com.civic.worker.dto;

import com.civic.worker.Worker;
import com.civic.worker.WorkerStatus;

import java.time.LocalDateTime;

public record WorkerResponse(Long id, String name, String email, String phone, String department,
                             WorkerStatus status, LocalDateTime createdAt,
                             long activeAssignments, long completedAssignments) {

    public static WorkerResponse from(Worker worker, long activeAssignments, long completedAssignments) {
        return new WorkerResponse(worker.getId(), worker.getName(), worker.getEmail(), worker.getPhone(),
                worker.getDepartment(), worker.getStatus(), worker.getCreatedAt(), activeAssignments, completedAssignments);
    }
}
