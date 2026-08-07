package com.example.demo;

import com.example.demo.storage.ImageNotFoundException;
import com.example.demo.storage.S3ImageStorageBackend;
import com.example.demo.storage.StoredImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageBackendTests {
    private static final String BUCKET = "yu-bazaar-images";
    private static final String KEY = "00000000-0000-0000-0000-000000000000.png";

    @Mock
    private S3Client s3Client;

    private S3ImageStorageBackend backend;

    @BeforeEach
    void setUp() {
        backend = new S3ImageStorageBackend(s3Client, BUCKET);
    }

    @Test
    void uploadUsesConfiguredBucketKeyAndContentType() {
        byte[] content = new byte[]{1, 2, 3};
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        backend.put(KEY, "image/png", content.length, new ByteArrayInputStream(content));

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(request.getValue().key()).isEqualTo(KEY);
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void downloadPreservesBytesAndContentType() {
        byte[] content = new byte[]{4, 5, 6};
        GetObjectResponse response = GetObjectResponse.builder().contentType("image/png").build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(response, content));

        StoredImage image = backend.load(KEY);

        assertThat(image.content()).isEqualTo(content);
        assertThat(image.contentType()).isEqualTo("image/png");
    }

    @Test
    void missingObjectBecomesNotFound() {
        S3Exception missing = (S3Exception) S3Exception.builder()
                .statusCode(404)
                .message("Not found")
                .build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(missing);

        assertThatThrownBy(() -> backend.load(KEY))
                .isInstanceOf(ImageNotFoundException.class);
    }
}
