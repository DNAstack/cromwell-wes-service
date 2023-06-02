package com.dnastack.wes.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@ConditionalOnExpression("${wes.workflows.authorizers.checksum.enabled:false}")
public class ChecksumWorkflowAuthorizer implements WorkflowAuthorizer {


    private final Set<String> allowedChecksums;
    private final Set<String> excludedFiles;

    public ChecksumWorkflowAuthorizer(ChecksumWorkflowAuthorizerConfig config) {
        this.allowedChecksums = new HashSet<>(config.getAllowedChecksums());
        this.excludedFiles = new HashSet<>(config.getExcludedFiles());
    }

    @Override
    public boolean authorize(String url, MultipartFile[] contents) {

        URI workflowUri = URI.create(url);
        if (workflowUri.isAbsolute()) {
            return false;
        }


        return Stream.of(contents)
            .filter(file -> !excludedFiles.contains(file.getOriginalFilename()))
            .allMatch(this::compareChecksum);
    }

    private boolean compareChecksum(MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            byte[] allBytes = inputStream.readAllBytes();
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5Digest.update(allBytes);
            String computedMd5Sum = DatatypeConverter.printHexBinary(md5Digest.digest()).toLowerCase();
            return this.allowedChecksums.contains(computedMd5Sum);
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Configuration
    @ConfigurationProperties("wes.workflows.authorizers.checksum")
    public static class ChecksumWorkflowAuthorizerConfig {

        List<String> allowedChecksums = new ArrayList<>();

        // Provide the ability to exclude specific safe named files
        List<String> excludedFiles = new ArrayList<>();


    }

}
