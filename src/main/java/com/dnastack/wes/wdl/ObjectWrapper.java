package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = ObjectWrapperSerializer.class)
public class ObjectWrapper {

    @Getter
    JsonNode originalValue;

    @Setter
    JsonNode mappedValue;

    @Getter
    @Setter
    String transferDestination;

    @Getter
    @Setter
    String sourceDestination;

    @Getter
    @Setter
    String accessToken;

    @Setter
    @Getter
    Boolean wasMapped = false;

    @Setter
    @Getter
    Boolean requiresTransfer = false;

    public ObjectWrapper(JsonNode originalValue) {
        this.originalValue = originalValue;
    }


    public JsonNode getMappedvalue() {
        if (mappedValue == null) {
            return originalValue;
        } else {
            return mappedValue;
        }
    }


}
