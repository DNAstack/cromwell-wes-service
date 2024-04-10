package com.dnastack.wes.utils;

import com.dnastack.wes.agent.Base64EncodedAttachments;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MultipartUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MultipartUtils() {
    }

    public static MultipartFile getAsJsonMultipart(Object object) {
        return getAsJsonMultipart(null, object).orElse(null);
    }

    public static Optional<MultipartFile> getAsJsonMultipart(String name, Object object) {

        if (object == null) {
            return Optional.empty();
        }

        try {
            byte[] objectBytes = MAPPER.writeValueAsBytes(object);
            return Optional.of(getAsMultipart(name, MediaType.APPLICATION_JSON_VALUE, objectBytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MultipartFile getAsMultipart(byte[] bytes) {
        return getAsMultipart(null, MediaType.APPLICATION_OCTET_STREAM_VALUE, bytes);
    }

    public static MultipartFile getAsMultipart(String name, String mediaType, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return ByteArrayMultipartFile.builder()
            .bytes(bytes)
            .contentType(mediaType)
            .name(name)
            .build();
    }

    public static MultipartFile[] getAsMultipart(List<Base64EncodedAttachments> attachments) {
        return attachments.stream()
            .map(workflowFile -> MultipartUtils.ByteArrayMultipartFile.builder()
                .name(workflowFile.path())
                .originalFilename(workflowFile.path())
                .bytes(Base64.getDecoder().decode(workflowFile.content()))
                .contentType(workflowFile.contentType())
                .build())
            .toArray(MultipartFile[]::new);
    }

    public static List<FormParameter> zipParametersAndFiles(MultipartHttpServletRequest request) throws IOException {
        List<FormParameter> zippedFormParams = new ArrayList<>();
        for (Map.Entry<String, MultipartFile> entry : request.getFileMap().entrySet()) {
            MultipartFile file = entry.getValue();
            zippedFormParams.add(FormParameter.builder().formName(entry.getKey()).mimeType(file.getContentType()).fileName(file.getOriginalFilename())
                .inputStream(file.getInputStream()).build());
        }

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            for (String val : values) {
                zippedFormParams.add(
                    FormParameter.builder().formName(entry.getKey()).inputStream(new ByteArrayInputStream(val.getBytes(StandardCharsets.UTF_8))).build());
            }
        }
        return zippedFormParams;
    }

    public static <T> T readFileContents(InputStream stream, Class<T> clazz) throws UncheckedIOException {
        if (stream != null) {
            try {
                return MAPPER.readValue(stream, clazz);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return null;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ByteArrayMultipartFile implements MultipartFile {

        String name;

        String originalFilename;

        String contentType;

        byte[] bytes;

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            try (OutputStream outputStream = new FileOutputStream(destination)) {
                outputStream.write(bytes);
            }
        }

    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormParameter {

        private String formName;
        private String fileName;
        private String mimeType;
        private InputStream inputStream;

    }

}
