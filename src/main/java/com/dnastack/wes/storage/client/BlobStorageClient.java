package com.dnastack.wes.storage.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public interface BlobStorageClient {

    URL getSignedUrl(String blobUri);

    default void getBytes(OutputStream outputStream, String blobUri) throws IOException {
        getAndWriteBytes(outputStream, blobUri, null, null);
    }


    void getAndWriteBytes(OutputStream outputStream, String blobUri, Long rangeStart, Long rangeEnd) throws IOException;
}
