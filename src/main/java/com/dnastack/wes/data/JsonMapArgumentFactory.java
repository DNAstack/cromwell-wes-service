package com.dnastack.wes.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.sql.Types;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.config.ConfigRegistry;

public class JsonMapArgumentFactory implements ArgumentFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Optional<Argument> NULL_ARGUMENT = Optional.of(new NullArgument(Types.VARCHAR));

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        try {
            if (value instanceof Map) {
                return Optional
                    .of(ObjectArgument.of(objectMapper.writeValueAsString(value), Types.VARCHAR));
            }
            if (value == null && type.getTypeName().equals("java.util.Map")) {
                return NULL_ARGUMENT;
            }
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        return Optional.empty();
    }
}