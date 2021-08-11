package com.dnastack.wes.transfer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

public class TrackedTransferRowMapper implements RowMapper<TrackedTransfer> {


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public TrackedTransfer map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TrackedTransfer(
            rs.getTimestamp("last_update").toLocalDateTime().atZone(ZoneOffset.UTC),
            rs.getTimestamp("created").toLocalDateTime().atZone(ZoneOffset.UTC),
            rs.getString("cromwell_id"),
            getJsonOrEmpty(rs.getString("transfer_job_ids")),
            rs.getInt("failure_attempts"));
    }

    private List<String> getJsonOrEmpty(String json) throws SQLException {

        if (json == null) {
            return Collections.emptyList();
        }

        TypeReference<List<String>> type = new TypeReference<>() {
        };

        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new SQLException("Unable to map '" + json + "' to " + type, e);
        }
    }

}