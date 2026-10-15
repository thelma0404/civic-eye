package com.civic.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Saves uploaded images under the uploads folder and returns their public URL. */
@Component
public class FileUploadUtil {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");

    private final Path root;

    public FileUploadUtil(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * @param folder sub-folder, e.g. "issues" or "work-proof"
     * @return web path such as /uploads/issues/3f2c....jpg
     */
    public String saveFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image to upload");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP or GIF images are allowed");
        }
        String extension = switch (type) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        try {
            Path dir = root.resolve(folder).normalize();
            if (!dir.startsWith(root)) {
                throw new IllegalArgumentException("Invalid upload folder");
            }
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + folder + "/" + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the uploaded file", e);
        }
    }

    public Path getRoot() {
        return root;
    }
}
