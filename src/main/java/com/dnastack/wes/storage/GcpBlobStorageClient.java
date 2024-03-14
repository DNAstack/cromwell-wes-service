package com.dnastack.wes.storage;

import com.dnastack.wes.shared.ConfigurationException;
import com.dnastack.wes.shared.NotFoundException;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Blob.BlobSourceOption;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.google.cloud.storage.Storage.SignUrlOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRange;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GcpBlobStorageClient implements BlobStorageClient {

    private final Storage client;
    private final long ttl;
    private final URI stagingLocation;
    private final String project;

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

        client = builder.setProjectId(config.getProject())
            .setQuotaProjectId(config.getBillingProject() == null ? config.getProject() : config.getBillingProject())
            .build()
            .getService();
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
    public void readBytes(OutputStream outputStream, String blobUri, HttpRange httpRange) throws IOException {
        BlobId blobId = GcpStorageUtils.blobIdFromGsUrl(blobUri);
        Blob blob = client.get(blobId);

        if (blob == null || !blob.exists()) {
            throw new FileNotFoundException("Could not open open file: " + blobUri + " it does not appear to exist");
        }

        long rangeStart = 0L;
        long rangeEnd = blob.getSize();

        if (httpRange != null) {
            rangeStart = httpRange.getRangeStart(blob.getSize());
            // httpRange is inclusive, but google limit is not
            rangeEnd = httpRange.getRangeEnd(blob.getSize()) + 1;
        }

        try (ReadChannel readChannel = blob.reader(BlobSourceOption.userProject(project))) {
            readChannel.seek(rangeStart);
            readChannel.limit(rangeEnd);
            // the outer stream is
            try (InputStream inputStream = Channels.newInputStream(readChannel)) {
                inputStream.transferTo(outputStream);
            }
        }
    }

    @Override
    public boolean isFile(String filePath) {
        try {
            return client.get(GcpStorageUtils.blobIdFromGsUrl(filePath)).exists();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String filePath) {
        client.delete(GcpStorageUtils.blobIdFromGsUrl(filePath));
    }

    @Override
    public BlobMetadata getBlobMetadata(String filePath) {
        Blob blob = client.get(GcpStorageUtils.blobIdFromGsUrl(filePath));
        if (Boolean.FALSE.equals(blob.exists())) {
            throw new NotFoundException("File: " + filePath + ", does does not exist");
        }

        return BlobMetadata.builder().name(blob.getName())
            .contentType(blob.getContentType())
            .contentEncoding(blob.getContentEncoding())
            .size(blob.getSize())
            .creationTime(TimeUtils.offsetToInstant(blob.getCreateTimeOffsetDateTime()))
            .lastModifiedTime(TimeUtils.offsetToInstant(blob.getUpdateTimeOffsetDateTime())).build();
    }

}
