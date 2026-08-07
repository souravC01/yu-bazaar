package com.example.demo.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImageStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageStorageService.class);
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Pattern IMAGE_KEY = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|gif|webp)$"
    );
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final ImageStorageBackend backend;

    public ImageStorageService(ImageStorageBackend backend) {
        this.backend = backend;
    }

    public String store(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Select an image for the listing.");
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("The image must be 5 MB or smaller.");
        }

        String contentType = imageFile.getContentType() == null
                ? ""
                : imageFile.getContentType().toLowerCase(Locale.ROOT);
        String extension = IMAGE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Upload a JPG, PNG, GIF, or WebP image.");
        }

        String key = UUID.randomUUID() + extension;
        try (InputStream inputStream = imageFile.getInputStream()) {
            backend.put(key, contentType, imageFile.getSize(), inputStream);
            return key;
        } catch (IOException exception) {
            throw new ImageStorageException("Could not read the uploaded image", exception);
        }
    }

    public StoredImage load(String key) {
        requireValidKey(key);
        return backend.load(key);
    }

    public void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            requireValidKey(key);
            backend.delete(key);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not remove stored image {}", key, exception);
        }
    }

    private void requireValidKey(String key) {
        if (key == null || !IMAGE_KEY.matcher(key).matches()) {
            throw new ImageNotFoundException();
        }
    }
}
