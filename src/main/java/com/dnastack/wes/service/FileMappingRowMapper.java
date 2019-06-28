package com.dnastack.wes.service;

import com.dnastack.wes.model.FileMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class FileMappingRowMapper implements RowMapper<FileMapping> {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public FileMapping map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new FileMapping(
            rs.getString("id"),
            getJsonOrNull(rs.getString("mapping"))
        );
    }

    private Map<String, Object> getJsonOrNull(String json) throws SQLException {

        if (json == null) {
            return Collections.emptyMap();
        }

        TypeReference<Map<String, Object>> type = new TypeReference<>() {
        };

        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new SQLException("Unable to map '" + json + "' to " + type, e);
        }
    }
}
