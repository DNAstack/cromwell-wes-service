package com.dnastack.wes.storage;

import com.dnastack.wes.shared.ConfigurationException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Blob.BlobSourceOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.google.cloud.storage.Storage.SignUrlOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GcpBlobStorageClient implements BlobStorageClient {

    private Storage client;
    private long ttl;
    private URI stagingLocation;
    private String project;

    public GcpBlobStorageClient(GcpBlobStorageConfig config) throws IOException {
        StorageOptions.Builder builder;

        if (config.getProject() == null) {
            throw new ConfigurationException("GCP project for blob storage client cannot be null");
        } else if (config.getStagingLocation() == null) {
            throw new ConfigurationException("GCP staging location for blob storage client cannot be null");
        }

        if (config.getServiceAccountJson() != null) {
            builder = StorageOptions.newBuilder().setCredentials(ServiceAccountCredentials
                .fromStream(new ByteArrayInputStream(config.getServiceAccountJson().getBytes())));
        } else {
            builder = StorageOptions.getDefaultInstance().toBuilder();
        }

        client = builder.setProjectId(config.getProject()).build().getService();
        ttl = config.getSigndUrlTtl().toMillis();
        stagingLocation = config.getStagingLocation();
        project = config.getProject();
    }


    @Override
    public URL getSignedUrl(String blobUri) {
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);
        return blob.signUrl(ttl, TimeUnit.MILLISECONDS, SignUrlOption.httpMethod(HttpMethod.GET), SignUrlOption
            .httpMethod(HttpMethod.HEAD));
    }

    @Override
    public String writeBytes(InputStream blobStream, long size, String stagingFolder, String blobName) throws IOException {
        String blobUri = UriComponentsBuilder.fromUri(stagingLocation).pathSegment(stagingFolder, blobName)
            .toUriString();
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);
        if (blob != null && blob.exists()) {
            throw new IOException("An in the current staging directory with the name: " + blobName
                                  + " already exists. Could not overrwrite file");
        }

        blob = client.create(BlobInfo.newBuilder(blobId).build());
        try (WriteChannel writeChannel = blob.writer(BlobWriteOption.userProject(project))) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(64 * 1024);
            while (blobStream.available() > 0) {
                byteBuffer.put(blobStream.readNBytes(64 * 1024));
                byteBuffer.flip();
                writeChannel.write(byteBuffer);
                byteBuffer.compact();
            }
            blobStream.close();
        }
        return blobUri;
    }

    @Override
    public void readBytes(OutputStream outputStream, String blobUri, Long rangeStart, Long rangeEnd) throws IOException {
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);

        if (blob == null || !blob.exists()) {
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
            try (ReadChannel reader = blob.reader(BlobSourceOption.userProject(project))) {
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
