package com.dnastack.wes.wdl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class ObjectWrapperSerializer extends JsonSerializer<ObjectWrapper> {

    @Override
    public void serialize(ObjectWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObject(value.getMappedvalue());
    }
}
