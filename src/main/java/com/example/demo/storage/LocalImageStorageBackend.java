package com.example.demo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageBackend implements ImageStorageBackend {
    private final Path uploadDirectory;

    public LocalImageStorageBackend(@Value("${app.upload.directory}") String uploadDirectory) {
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, String contentType, long contentLength, InputStream inputStream) {
        Path destination = resolve(key);
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ImageStorageException("Could not store image", exception);
        }
    }

    @Override
    public StoredImage load(String key) {
        Path image = resolve(key);
        if (!Files.isRegularFile(image)) {
            throw new ImageNotFoundException();
        }
        try {
            return new StoredImage(Files.readAllBytes(image), contentTypeFor(key));
        } catch (IOException exception) {
            throw new ImageStorageException("Could not load image", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new ImageStorageException("Could not delete image", exception);
        }
    }

    private Path resolve(String key) {
        Path path = uploadDirectory.resolve(key).normalize();
        if (!path.startsWith(uploadDirectory)) {
            throw new ImageNotFoundException();
        }
        return path;
    }

    private String contentTypeFor(String key) {
        if (key.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (key.endsWith(".png")) {
            return "image/png";
        }
        if (key.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/webp";
    }
}
