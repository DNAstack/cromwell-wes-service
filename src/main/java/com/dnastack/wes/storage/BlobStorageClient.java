package com.dnastack.wes.storage;

import org.springframework.http.HttpRange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public interface BlobStorageClient {

    URL getSignedUrl(String blobUri);

    String writeBytes(InputStream stream, long uploadSize, String stagingFolder, String fileName) throws IOException;

    default void getBytes(OutputStream outputStream, String blobUri) throws IOException {
        readBytes(outputStream, blobUri, null);
    }

    void readBytes(OutputStream outputStream, String blobUri, @Nullable HttpRange  httpRange) throws IOException;

    boolean isFile(String filePath);

}
