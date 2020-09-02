package com.dnastack.wes.storage.client.gcp;

import com.dnastack.wes.config.GcpBlobStorageConfig;
import com.dnastack.wes.storage.client.BlobStorageClient;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GcpBlobStorageClient implements BlobStorageClient {

    private Storage client;
    private long ttl;

    public GcpBlobStorageClient(GcpBlobStorageConfig config) throws IOException {
        StorageOptions.Builder builder = null;
        if (config.getServiceAccountJson() != null) {
            builder = StorageOptions.newBuilder().setCredentials(ServiceAccountCredentials
                .fromStream(new ByteArrayInputStream(config.getServiceAccountJson().getBytes())));
        } else {
            builder = StorageOptions.getDefaultInstance().toBuilder();
        }

        client = builder.setProjectId(config.getProject()).build().getService();
        ttl = config.getSigndUrlTtl().toMillis();
    }


    @Override
    public URL getSignedUrl(String blobUri) {
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);
        return blob.signUrl(ttl, TimeUnit.MILLISECONDS, SignUrlOption.httpMethod(HttpMethod.GET), SignUrlOption
            .httpMethod(HttpMethod.HEAD));
    }

    @Override
    public void getAndWriteBytes(OutputStream outputStream, String blobUri, Long rangeStart, Long rangeEnd) throws IOException {
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);

        if (blob == null || !blob.exists()){
            throw new FileNotFoundException("Could not open open file: " + blobUri + " it does not appear to exist");
        }

        if (rangeStart == null) {
            rangeStart = 0L;
        }

        if (rangeEnd == null) {
            rangeEnd = blob.getSize();
        }

        long totalBytesToRead = rangeEnd - rangeStart;
        int bufferSize = 64 * 1024;

        if (totalBytesToRead <= bufferSize) {
            outputStream.write(blob.getContent());
        } else {
            try (ReadChannel reader = blob.reader()) {
                reader.seek(rangeStart);
                WritableByteChannel writer = Channels.newChannel(outputStream);
                long maxRead = totalBytesToRead - bufferSize;

                ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
                int totalBytes = 0;
                while ((totalBytes += reader.read(byteBuffer)) < maxRead) {
                    byteBuffer.flip();
                    writer.write(byteBuffer);
                    byteBuffer.clear();
                }

                if (totalBytes < totalBytesToRead) {
                    byteBuffer = ByteBuffer.allocate((int) totalBytesToRead - totalBytes);
                    reader.read(byteBuffer);
                    byteBuffer.flip();
                    writer.write(byteBuffer);
                    byteBuffer.clear();
                }
            }
        }

    }
}
