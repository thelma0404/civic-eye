package com.civic.worker;

import com.civic.assignment.AssignmentRepository;
import com.civic.assignment.AssignmentStatus;
import com.civic.common.exception.WorkerNotFoundException;
import com.civic.common.util.TextUtil;
import com.civic.worker.dto.WorkerRequest;
import com.civic.worker.dto.WorkerResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerService {

    private static final List<AssignmentStatus> ACTIVE = List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS);

    private final WorkerRepository workerRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkerService(WorkerRepository workerRepository, AssignmentRepository assignmentRepository,
                         PasswordEncoder passwordEncoder) {
        this.workerRepository = workerRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<WorkerResponse> getAllWorkers() {
        return workerRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    /** Workers who can receive new complaints (everyone who isn't inactive). */
    public List<WorkerResponse> getAssignableWorkers() {
        return workerRepository.findByStatusNotOrderByNameAsc(WorkerStatus.INACTIVE).stream().map(this::toResponse).toList();
    }

    public Worker findWorker(Long id) {
        return workerRepository.findById(id).orElseThrow(() -> new WorkerNotFoundException(id));
    }

    public WorkerResponse getWorker(Long id) {
        return toResponse(findWorker(id));
    }

    @Transactional
    public WorkerResponse createWorker(WorkerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Worker details are required");
        }
        String name = TextUtil.clean(request.name());
        String email = TextUtil.normalizeEmail(request.email());
        if (name == null) {
            throw new IllegalArgumentException("Worker name is required");
        }
        if (!TextUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (workerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A worker with this email already exists");
        }
        Worker worker = new Worker();
        worker.setName(name);
        worker.setEmail(email);
        worker.setPhone(TextUtil.clean(request.phone()));
        worker.setDepartment(TextUtil.clean(request.department()));
        worker.setPassword(passwordEncoder.encode(request.password()));
        worker.setStatus(request.status() == null ? WorkerStatus.AVAILABLE : request.status());
        return toResponse(workerRepository.save(worker));
    }

    @Transactional
    public WorkerResponse updateWorker(Long id, WorkerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Worker details are required");
        }
        Worker worker = findWorker(id);
        String name = TextUtil.clean(request.name());
        if (name != null) {
            worker.setName(name);
        }
        String email = TextUtil.normalizeEmail(request.email());
        if (email != null && !email.equals(worker.getEmail())) {
            if (!TextUtil.isValidEmail(email)) {
                throw new IllegalArgumentException("Please enter a valid email address");
            }
            if (workerRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("A worker with this email already exists");
            }
            worker.setEmail(email);
        }
        worker.setPhone(TextUtil.clean(request.phone()));
        worker.setDepartment(TextUtil.clean(request.department()));
        if (request.status() != null && request.status() != worker.getStatus()) {
            if (request.status() == WorkerStatus.INACTIVE && activeCount(id) > 0) {
                throw new IllegalArgumentException("This worker still has open assignments. Reassign them before deactivating.");
            }
            worker.setStatus(request.status());
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            worker.setPassword(passwordEncoder.encode(request.password()));
        }
        return toResponse(workerRepository.save(worker));
    }

    /** Workers are deactivated rather than deleted so their work history stays intact. */
    @Transactional
    public void deactivateWorker(Long id) {
        Worker worker = findWorker(id);
        if (activeCount(id) > 0) {
            throw new IllegalArgumentException("This worker still has open assignments. Reassign them before deactivating.");
        }
        worker.setStatus(WorkerStatus.INACTIVE);
        workerRepository.save(worker);
    }

    @Transactional
    public WorkerResponse updateProfile(Long id, String name, String phone) {
        Worker worker = findWorker(id);
        String cleanName = TextUtil.clean(name);
        if (cleanName == null) {
            throw new IllegalArgumentException("Name can't be empty");
        }
        worker.setName(cleanName);
        worker.setPhone(TextUtil.clean(phone));
        return toResponse(workerRepository.save(worker));
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        Worker worker = findWorker(id);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, worker.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        worker.setPassword(passwordEncoder.encode(newPassword));
        workerRepository.save(worker);
    }

    private long activeCount(Long workerId) {
        return assignmentRepository.countByWorkerIdAndStatusIn(workerId, ACTIVE);
    }

    private WorkerResponse toResponse(Worker worker) {
        return WorkerResponse.from(worker, activeCount(worker.getId()),
                assignmentRepository.countByWorkerIdAndStatus(worker.getId(), AssignmentStatus.COMPLETED));
    }
}
