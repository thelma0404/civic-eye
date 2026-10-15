package com.civic.complaint;

import com.civic.common.util.FileUploadUtil;
import com.civic.common.util.TextUtil;
import com.civic.notification.NotificationService;
import com.civic.user.User;
import com.civic.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final AiCategoryService aiCategoryService;
    private final FileUploadUtil fileUploadUtil;
    private final NotificationService notificationService;

    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository,
                            AiCategoryService aiCategoryService, FileUploadUtil fileUploadUtil,
                            NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.aiCategoryService = aiCategoryService;
        this.fileUploadUtil = fileUploadUtil;
        this.notificationService = notificationService;
    }

    @Transactional
    public Complaint createComplaint(Long citizenId, String title, String description, String category,
                                     String location, MultipartFile image) {
        String t = TextUtil.clean(title);
        String d = TextUtil.clean(description);
        String l = TextUtil.clean(location);
        if (t == null || t.length() < 5) {
            throw new IllegalArgumentException("Please give the issue a short title (at least 5 characters)");
        }
        if (t.length() > 150) {
            throw new IllegalArgumentException("The title must be 150 characters or fewer");
        }
        if (d == null || d.length() < 10) {
            throw new IllegalArgumentException("Please describe the issue in a little more detail (at least 10 characters)");
        }
        if (d.length() > 2000) {
            throw new IllegalArgumentException("The description must be 2000 characters or fewer");
        }
        if (l == null) {
            throw new IllegalArgumentException("Please tell us where the issue is");
        }
        if (l.length() > 255) {
            throw new IllegalArgumentException("The location must be 255 characters or fewer");
        }
        User citizen = userRepository.findById(citizenId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        AiCategoryService.Result ai = aiCategoryService.classify(t, d);
        String chosen = TextUtil.clean(category);
        boolean useAi = chosen == null || "AUTO".equalsIgnoreCase(chosen) || !aiCategoryService.categories().contains(chosen);

        Complaint complaint = new Complaint();
        complaint.setTitle(t);
        complaint.setDescription(d);
        complaint.setLocation(l);
        complaint.setCitizen(citizen);
        complaint.setAiCategory(ai.category());
        complaint.setCategory(useAi ? ai.category() : chosen);
        complaint.setPriority(ai.priority());
        complaint.setStatus(Complaint.Status.PENDING);
        if (image != null && !image.isEmpty()) {
            complaint.setImagePath(fileUploadUtil.saveFile(image, "issues"));
        }
        Complaint saved = complaintRepository.save(complaint);

        notificationService.notifyCitizen(citizenId, "Complaint registered",
                "Your complaint #" + saved.getId() + " \"" + t + "\" has been received. We'll keep you posted.");
        notificationService.notifyAdmins("New complaint #" + saved.getId(),
                t + " · " + saved.getCategory() + " · " + saved.getPriority() + " priority");
        return saved;
    }

    public List<Complaint> getCitizenComplaints(Long citizenId) {
        return complaintRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId);
    }

    public Complaint getCitizenComplaint(Long citizenId, Long complaintId) {
        return complaintRepository.findByIdAndCitizenId(complaintId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
    }

    public Complaint getComplaint(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
    }
}
