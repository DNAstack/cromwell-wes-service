package com.dnastack.wes.storage.client.local;

import com.dnastack.wes.storage.LocalBlobStorageClient;
import com.dnastack.wes.storage.LocalBlobStorageClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class LocalBlobStorageClientTest {

    private final String toWrite = "Test String";
    private final String directory = "test";
    private final String fileName = "test.txt";

    @Test
    public void testCreateStorageClient_noStagingPath() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        String stagingPath = storageClient.getStagingPath();
        Assertions.assertTrue(Files.exists(Path.of(stagingPath)));
    }

    @Test
    public void testCreateStorageClient_withStagingPath() throws IOException {
        LocalBlobStorageClientConfig config = new LocalBlobStorageClientConfig();
        Path tempDirectory = Files.createTempDirectory("testCreateStorageClient_withStagingPath");
        config.setStagingPath(tempDirectory.toString());
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient(config);
        Assertions.assertEquals(tempDirectory.toString(), storageClient.getStagingPath());
        Assertions.assertTrue(Files.exists(Path.of(storageClient.getStagingPath())));
    }

    @Test
    public void testWritingBytesToFile() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        storageClient.writeBytes(new ByteArrayInputStream(toWrite.getBytes()), toWrite.length(), directory, fileName);

        Assertions.assertTrue(Files.exists(targetPath));
        Assertions.assertEquals(toWrite, Files.readString(targetPath));
    }


    @Test
    public void testWritingBytesToFile_existingFileThrowsError() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        Files.createDirectory(targetPath.getParent());
        Files.write(targetPath, toWrite.getBytes(), StandardOpenOption.CREATE_NEW);

        Assertions.assertThrows(IOException.class, () -> storageClient
            .writeBytes(new ByteArrayInputStream(toWrite.getBytes()), toWrite.length(), directory, fileName));
    }


    @Test
    public void testReadingFile() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        Files.createDirectory(targetPath.getParent());
        Files.write(targetPath, toWrite.getBytes(), StandardOpenOption.CREATE_NEW);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        storageClient.readBytes(outputStream, targetPath.toString(), HttpRange.createByteRange(0L,toWrite.length()));
        String readValue = outputStream.toString();
        Assertions.assertEquals(readValue, toWrite);

    }

    @Test
    public void testReadingFile_withTruncation() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        Files.createDirectory(targetPath.getParent());
        Files.write(targetPath, toWrite.getBytes(), StandardOpenOption.CREATE_NEW);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        storageClient.readBytes(outputStream, targetPath.toString(), HttpRange.createByteRange(5L,toWrite.length()));
        String readValue = outputStream.toString();
        Assertions.assertEquals(toWrite.substring(5), readValue);
    }

    @Test
    public void testIsFile() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        Files.createDirectory(targetPath.getParent());
        Files.write(targetPath, toWrite.getBytes(), StandardOpenOption.CREATE_NEW);

        Assertions.assertTrue(storageClient.isFile(targetPath.toString()));
    }

    @Test
    public void testIsFile_noFileExists() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);

        Assertions.assertFalse(storageClient.isFile(targetPath.toString()));
    }

    @Test
    public void testDeletingFile() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);
        Files.createDirectory(targetPath.getParent());
        Files.write(targetPath, toWrite.getBytes(), StandardOpenOption.CREATE_NEW);

        Assertions.assertTrue(Files.exists(targetPath));
        storageClient.deleteFile(targetPath.toString());
        Assertions.assertFalse(Files.exists(targetPath));
    }

    @Test
    public void testDeletingFile_throwsError() throws IOException {
        LocalBlobStorageClient storageClient = new LocalBlobStorageClient();
        Path targetPath = Path.of(storageClient.getStagingPath() + "/" + directory + "/" + fileName);

        Assertions.assertFalse(Files.exists(targetPath));
        Assertions.assertThrows(IOException.class, () -> storageClient.deleteFile(targetPath.toString()));
    }

}