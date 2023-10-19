package com.dnastack.wes.storage;

import com.dnastack.wes.shared.ConfigurationException;
import org.springframework.http.HttpRange;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            throw new StorageException(e);
        }
    }

    @Override
    public String writeBytes(InputStream stream, long uploadSize, String stagingFolder, String fileName) throws IOException {

        Path path = Paths.get(stagingPath, stagingFolder ,fileName);
        File fileToWrite = path.toFile();
        String filePath = fileToWrite.getAbsolutePath();

        if (!filePath.startsWith(stagingPath)) {
            throw new IOException("Could not write to file path " + filePath + ". Path outside of protected scope");
        }

        if (fileToWrite.exists()) {
            throw new IOException("Could not write to file path " + filePath + ". File already exists");
        }

        fileToWrite.getParentFile().mkdirs();

        try (FileOutputStream fileOutputStream = new FileOutputStream(fileToWrite)) {
            stream.transferTo(fileOutputStream);
        }

        return filePath;
    }

    @Override
    public void readBytes(OutputStream outputStream, String blobUri, HttpRange httpRange) throws IOException {
        File fileToRead = new File(blobUri);
        if (!fileToRead.exists()) {
            throw new IOException("Could not read from file: " + fileToRead + ". File does not exist");
        }

        long rangeStart = 0L;
        long rangeEnd = fileToRead.length();
        if (httpRange != null){
            rangeStart = httpRange.getRangeStart(fileToRead.length());
        }

        try (FileChannel channel = new RandomAccessFile(fileToRead, "r").getChannel()) {
            channel.position(rangeStart);
            try (InputStream inputStream = new BoundedInputStream(Channels.newInputStream(channel),rangeEnd - rangeStart)) {
                inputStream.transferTo(outputStream);
            }
        }
    }

    @Override
    public boolean isFile(String filePath) {
        try {
            return filePath.startsWith("/") && Files.exists(Path.of(filePath));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Files.delete(Path.of(filePath));
    }

}
