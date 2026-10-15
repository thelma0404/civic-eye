package com.civic.worker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Worker> findAllByOrderByNameAsc();

    List<Worker> findByStatusOrderByNameAsc(WorkerStatus status);

    List<Worker> findByStatusNotOrderByNameAsc(WorkerStatus status);

    long countByStatus(WorkerStatus status);
}
