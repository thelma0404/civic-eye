package com.civic;

import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CivicEyeApplication {

    private static final Logger log = LoggerFactory.getLogger(CivicEyeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CivicEyeApplication.class, args);
    }

    /** Creates a default admin and a demo field worker the first time the app starts. */
    @Bean
    CommandLineRunner seedDefaultAccounts(UserRepository users, WorkerRepository workers, PasswordEncoder encoder) {
        return args -> {
            User admin = users.findByEmail("admin@civic.com").orElse(null);
            if (admin == null) {
                admin = new User();
                admin.setName("System Admin");
                admin.setEmail("admin@civic.com");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                users.save(admin);
                log.info("Created default admin account -> admin@civic.com / admin123");
            } else if (!admin.isActive() || admin.getRole() != Role.ADMIN) {
                // Repair the default admin if it was disabled or changed, so you can't get locked out.
                admin.setActive(true);
                admin.setRole(Role.ADMIN);
                users.save(admin);
                log.info("Re-enabled the default admin account (admin@civic.com)");
            }
            Worker worker = workers.findByEmail("worker@civic.com").orElse(null);
            if (worker == null) {
                worker = new Worker();
                worker.setName("Ravi Kumar");
                worker.setEmail("worker@civic.com");
                worker.setPassword(encoder.encode("worker123"));
                worker.setPhone("9876543210");
                worker.setDepartment("Roads & Potholes");
                worker.setStatus(WorkerStatus.AVAILABLE);
                workers.save(worker);
                log.info("Created demo worker account -> worker@civic.com / worker123");
            } else if (worker.getStatus() == null || worker.getStatus() == WorkerStatus.INACTIVE) {
                // Repair the demo worker if it was disabled, so it can always log in.
                worker.setStatus(WorkerStatus.AVAILABLE);
                workers.save(worker);
                log.info("Re-enabled the demo worker account (worker@civic.com)");
            }
            log.info("Civic Eye is running at http://localhost:8080");
        };
    }
}
