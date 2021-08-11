package com.dnastack.wes.storage;

import com.dnastack.wes.shared.ConfigurationException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalBlobStorageClient implements BlobStorageClient {

    private final String stagingPath;

    public LocalBlobStorageClient() throws IOException {
        this(new LocalBlobStorageClientConfig());
    }

    public LocalBlobStorageClient(LocalBlobStorageClientConfig config) throws IOException {
        if (config == null) {
            throw new ConfigurationException("Could not create LocalBlobStorageClient, no config provided");
        }

        if (config.getStagingPath() == null || config.getStagingPath().isEmpty()) {
            Path directory = Files.createTempDirectory("workflow_attachments");
            stagingPath = directory.toAbsolutePath().toString();
        } else {
            stagingPath = config.getStagingPath();
        }

    }


    public String getStagingPath() {
        return stagingPath;
    }

    @Override
    public URL getSignedUrl(String blobUri) {
        try {
            return URI.create(blobUri).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String writeBytes(InputStream stream, long uploadSize, String stagingFolder, String fileName) throws IOException {
        String path = stagingPath + "/" + stagingFolder + "/" + fileName;
        File fileToWrite = new File(path);
        String filePath = fileToWrite.getCanonicalPath();

        if (!filePath.startsWith(stagingPath)) {
            throw new IOException("Could not write to file path " + filePath + ". Path outset of protected scope");
        }

        if (fileToWrite.exists()) {
            throw new IOException("Could not write to file path " + filePath + ". File already exists");
        }

        fileToWrite.getParentFile().mkdirs();

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileToWrite)) {
            byte[] bytes = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = stream.read(bytes)) > 0) {
                fileOutputStream.write(bytes, 0, bytesRead);
            }
        }

        return filePath;
    }

    @Override
    public void readBytes(OutputStream outputStream, String blobUri, Long rangeStart, Long rangeEnd) throws IOException {
        File fileToRead = new File(blobUri);
        if (!fileToRead.exists()) {
            throw new IOException("Could not read from file: " + fileToRead + ". File does not exist");
        }

        if (rangeStart == null) {
            rangeStart = 0L;
        }

        if (rangeEnd == null) {
            rangeEnd = fileToRead.length();
        }


        try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileToRead, "r")) {
            randomAccessFile.seek(rangeStart);

            int byteArrayLength = 64 * 1024;

            if (rangeEnd - rangeStart < byteArrayLength) {
                byteArrayLength = rangeEnd.intValue() - rangeStart.intValue();
            }

            long bytesRemaining = rangeEnd - rangeStart;
            byte[] bytesIn = new byte[byteArrayLength];
            int bytesRead;
            while ((bytesRead = randomAccessFile.read(bytesIn)) > 0 && bytesRemaining > 0) {
                if (bytesRead > bytesRemaining) {
                    bytesRead = (int) bytesRemaining;
                }

                outputStream.write(bytesIn, 0, bytesRead);

                bytesRemaining -= bytesRead;
            }
        }
    }

}
