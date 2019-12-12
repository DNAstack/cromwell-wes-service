package com.dnastack.wes.service;

import com.dnastack.wes.model.OriginalInputs;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.springframework.stereotype.Component;

@Component
public class OriginalInputStore {

    private final Jdbi jdbi;

    OriginalInputStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    Map<String, Object> getInputs(String runId) {
        Optional<OriginalInputs> mapping = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM original_inputs where id = :id")
                .bind("id", runId)
                .registerRowMapper(new OriginalInputRowMapper())
                .mapTo(OriginalInputs.class)
                .findFirst()
        );
        if (mapping.isPresent()) {
            return mapping.get().getMapping();
        } else {
            return null;
        }
    }

    void saveInputs(String runId, Map<String, Object> inputs) {
        GenericType<Map<String, Object>> genericType = new GenericType<>() {
        };
        jdbi.withHandle(handle -> handle
            .createUpdate("INSERT INTO original_inputs(id,inputs) VALUES(:id,:inputs::jsonb)")
            .bind("id", runId)
            .bindByType("inputs", inputs, genericType)
            .execute());
    }

}
