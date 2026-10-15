package com.civic.common.exception;

public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(Long id) {
        super("Worker not found with id " + id);
    }
}
