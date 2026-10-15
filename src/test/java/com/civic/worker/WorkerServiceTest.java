package com.civic.worker;

import com.civic.assignment.AssignmentRepository;
import com.civic.common.exception.WorkerNotFoundException;
import com.civic.worker.dto.WorkerRequest;
import com.civic.worker.dto.WorkerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkerServiceTest {

    @Mock
    WorkerRepository workerRepository;
    @Mock
    AssignmentRepository assignmentRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    WorkerService workerService;

    private Worker worker(long id) {
        Worker w = new Worker();
        w.setId(id);
        w.setName("Ravi Kumar");
        w.setEmail("ravi@civic.com");
        w.setStatus(WorkerStatus.AVAILABLE);
        return w;
    }

    @Test
    void createWorker_encodesPasswordNormalisesEmailAndDefaultsToAvailable() {
        when(workerRepository.existsByEmail("asha@civic.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(workerRepository.save(any(Worker.class))).thenAnswer(inv -> {
            Worker w = inv.getArgument(0);
            w.setId(7L);
            return w;
        });

        WorkerResponse response = workerService.createWorker(
                new WorkerRequest("Asha Nair", "  Asha@Civic.com ", "9000000000", "Water Supply", "secret1", null));

        ArgumentCaptor<Worker> saved = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(saved.capture());
        assertEquals("hashed", saved.getValue().getPassword());
        assertEquals("asha@civic.com", saved.getValue().getEmail());
        assertEquals(WorkerStatus.AVAILABLE, response.status());
        assertEquals(7L, response.id());
    }

    @Test
    void createWorker_rejectsDuplicateEmail() {
        when(workerRepository.existsByEmail("ravi@civic.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> workerService.createWorker(
                new WorkerRequest("Ravi", "ravi@civic.com", null, null, "secret1", null)));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(workerRepository, never()).save(any());
    }

    @Test
    void createWorker_rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> workerService.createWorker(
                new WorkerRequest("Ravi", "ravi2@civic.com", null, null, "123", null)));
        verify(workerRepository, never()).save(any());
    }

    @Test
    void findWorker_throwsWhenMissing() {
        when(workerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(WorkerNotFoundException.class, () -> workerService.findWorker(99L));
    }

    @Test
    void deactivateWorker_isBlockedWhileAssignmentsAreOpen() {
        Worker w = worker(3L);
        when(workerRepository.findById(3L)).thenReturn(Optional.of(w));
        when(assignmentRepository.countByWorkerIdAndStatusIn(eq(3L), anyCollection())).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> workerService.deactivateWorker(3L));
        assertEquals(WorkerStatus.AVAILABLE, w.getStatus());
    }

    @Test
    void deactivateWorker_setsInactiveWhenIdle() {
        Worker w = worker(4L);
        when(workerRepository.findById(4L)).thenReturn(Optional.of(w));
        when(assignmentRepository.countByWorkerIdAndStatusIn(eq(4L), anyCollection())).thenReturn(0L);

        workerService.deactivateWorker(4L);

        assertEquals(WorkerStatus.INACTIVE, w.getStatus());
        verify(workerRepository).save(w);
    }
}
