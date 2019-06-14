package com.dnastack.wes.wdl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class FileWrapperSerializer extends JsonSerializer<FileWrapper> {

    @Override
    public void serialize(FileWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getMappedValue());
    }
}
