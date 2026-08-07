package com.example.demo.controller;

import com.example.demo.storage.ImageStorageService;
import com.example.demo.storage.StoredImage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;

@Controller
public class MediaController {
    private final ImageStorageService imageStorageService;

    public MediaController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/media/{key:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> loadImage(@PathVariable String key) {
        StoredImage image = imageStorageService.load(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(image.content());
    }
}
