package com.dnastack.wes.service;

import com.dnastack.wes.config.AppConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class LocalFileMappingStore implements FileMappingStore {

    private File mappingDir = null;
    private ObjectMapper mapper;

    @Autowired
    public LocalFileMappingStore(AppConfig config, ObjectMapper mapper) {
        this.mapper = mapper;
        mappingDir = new File(config.getFileMappingDirectory());
        if (!mappingDir.exists()) {
            if (!mappingDir.mkdirs()) {
                throw new RuntimeException("Could not configure mapping directory");
            }
        } else if (mappingDir.isFile()) {
            throw new RuntimeException("Could not configure mapping directory. A file exists with the same name");
        }
    }


    private File getMappedFile(String runId) {
        return Paths.get(mappingDir.getAbsolutePath(), String.format("%s-inputs.json", runId)).toFile();
    }

    @Override
    public Map<String, Object> getMapping(@NonNull String runId) {
        File localFile = getMappedFile(runId);
        if (localFile.exists()) {
            try {
                TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
                };
                return mapper.readValue(localFile, typeReference);
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
        return null;
    }


    @Override
    public void saveMapping(@NonNull String runId, @Nullable Map<String, Object> fileMapping) {
        File localfile = getMappedFile(runId);
        if (fileMapping != null && !fileMapping.isEmpty()) {
            try {
                mapper.writeValue(localfile, fileMapping);
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }
    }
}
