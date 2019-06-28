package com.dnastack.wes.service;

import com.dnastack.wes.model.FileMapping;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.springframework.stereotype.Component;

@Component
public class FileMappingStore {

    private final Jdbi jdbi;

    FileMappingStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    Map<String, Object> getMapping(String runId) {
        Optional<FileMapping> mapping = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM file_mappings where id = :id")
                .bind("id", runId)
                .registerRowMapper(new FileMappingRowMapper())
                .mapTo(FileMapping.class)
                .findFirst()
        );
        if (mapping.isPresent()) {
            return mapping.get().getMapping();
        } else {
            return null;
        }
    }

    void saveMapping(String runId, Map<String, Object> fileMapping) {
        GenericType<Map<String, Object>> genericType = new GenericType<>() {
        };
        jdbi.withHandle(handle -> handle
            .createUpdate("INSERT INTO file_mappings(id,mapping) VALUES(:id,:mapping::jsonb)")
            .bind("id", runId)
            .bindByType("mapping", fileMapping, genericType)
            .execute());
    }

}
