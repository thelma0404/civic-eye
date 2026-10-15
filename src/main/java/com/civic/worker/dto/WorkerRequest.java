package com.civic.worker.dto;

import com.civic.worker.WorkerStatus;

/** Data an admin sends to create or edit a worker. Password may be blank when editing. */
public record WorkerRequest(String name, String email, String phone, String department, String password,
                            WorkerStatus status) {
}
