package com.dnastack.wes.storage;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.dnastack.wes.shared.ConfigurationException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public class AzureBlobStorageClient implements BlobStorageClient {


    private final BlobServiceClient client;
    private final long signedUrlTtl;
    private final String container;
    private final String stagingPath;

    public AzureBlobStorageClient(AzureBlobStorageClientConfig config) {
        if (config == null) {
            throw new ConfigurationException("Could not create ABS client");
        }

        if (config.getConnectionString() != null) {

            client = new BlobServiceClientBuilder().connectionString(config.getConnectionString())
                .buildClient();
        } else {
            throw new ConfigurationException("ABS connection string required to build AzureBlobStorageClient");
        }

        if (config.getContainer() != null) {
            container = config.getContainer();
        } else {
            throw new ConfigurationException("Container required to build AzureBlobStorageClient");
        }

        signedUrlTtl = config.getSignedUrlTtl();
        stagingPath = config.getStagingPath();
    }


    @Override
    public URL getSignedUrl(String blobUri) {
        BlobUrlParts parts = BlobUrlParts.parse(blobUri);
        BlobContainerClient containerClient = client.getBlobContainerClient(parts.getBlobContainerName());
        BlobClient blobClient = containerClient.getBlobClient(parts.getBlobName());

        String sas = blobClient.generateSas(new BlobServiceSasSignatureValues(OffsetDateTime.now()
            .plus(signedUrlTtl, ChronoUnit.MILLIS), new BlobSasPermission().setReadPermission(true)));

        try {
            return UriComponentsBuilder.fromUriString(blobUri).replaceQuery(sas).build().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String writeBytes(InputStream stream, long size, String stagingFolder, String fileName) throws IOException {
        BlobContainerClient containerClient = client.getBlobContainerClient(container);

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        if (stagingPath != null) {
            builder.pathSegment(stagingPath);
        }

        String objectName = builder.pathSegment(stagingFolder, fileName).build().toString();
        BlobClient blobClient = containerClient.getBlobClient(objectName);
        if (blobClient.exists()) {
            throw new IOException("Could not write object " + fileName + "to target destination " + objectName
                                  + ". Object already exists");
        }

        blobClient.upload(new BufferedInputStream(stream), size);
        return blobClient.getBlobUrl();
    }

    @Override
    public void readBytes(OutputStream outputStream, String blobUri, Long rangeStart, Long rangeEnd) throws IOException {
        String containerName;
        String blobName;
        if (blobUri.startsWith("https")) {
            BlobUrlParts parts = BlobUrlParts.parse(blobUri);
            containerName = parts.getBlobContainerName();
            blobName = parts.getBlobName();
        } else {
            blobName = blobUri;
            containerName = container;
        }


        BlobContainerClient containerClient = client.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (!blobClient.exists()) {
            throw new IOException("Could not read from blob: " + blobUri + ", object does not exist");
        }

        if (rangeStart == null) {
            rangeStart = 0L;
        }

        if (rangeEnd == null) {
            rangeEnd = blobClient.getProperties().getBlobSize();
        }

        BlobRange range = new BlobRange(rangeStart, rangeEnd - rangeStart);
        blobClient.downloadWithResponse(outputStream, range, new DownloadRetryOptions()
            .setMaxRetryRequests(3), null, false, null, null);
    }

}
