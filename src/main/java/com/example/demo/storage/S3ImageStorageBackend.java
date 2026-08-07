package com.example.demo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ImageStorageBackend implements ImageStorageBackend {
    private final S3Client s3Client;
    private final String bucket;

    public S3ImageStorageBackend(S3Client s3Client,
                                 @Value("${app.storage.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, String contentType, long contentLength, InputStream inputStream) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (S3Exception exception) {
            throw new ImageStorageException("Could not store image", exception);
        } catch (SdkException exception) {
            throw new ImageStorageException("Could not connect to image storage", exception);
        }
    }

    @Override
    public StoredImage load(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            String contentType = response.response().contentType();
            return new StoredImage(
                    response.asByteArray(),
                    contentType == null ? "application/octet-stream" : contentType
            );
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new ImageNotFoundException(exception);
            }
            throw new ImageStorageException("Could not load image", exception);
        } catch (SdkException exception) {
            throw new ImageStorageException("Could not connect to image storage", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            throw new ImageStorageException("Could not delete image", exception);
        } catch (SdkException exception) {
            throw new ImageStorageException("Could not connect to image storage", exception);
        }
    }
}
