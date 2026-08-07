package com.example.demo.storage;

import java.io.InputStream;

public interface ImageStorageBackend {
    void put(String key, String contentType, long contentLength, InputStream inputStream);

    StoredImage load(String key);

    void delete(String key);
}
