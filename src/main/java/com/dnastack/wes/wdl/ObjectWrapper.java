package com.dnastack.wes.wdl;

import com.dnastack.wes.model.transfer.ExternalAccount;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = ObjectWrapperSerializer.class)
public class ObjectWrapper {

    @Getter
    JsonElement originalValue;

    @Setter
    String mappedValue;

    @Getter
    @Setter
    String transferDestination;

    @Getter
    @Setter
    String sourceDestination;

    @Getter
    @Setter
    String accessToken;

    @Getter
    @Setter
    ExternalAccount transferExternalAccount;

    @Setter
    @Getter
    Boolean wasMapped = false;

    @Setter
    @Getter
    Boolean requiresTransfer = false;

    public ObjectWrapper(JsonElement originalValue) {
        this.originalValue = originalValue;
    }

    public String getMappedValue() {
        if (mappedValue == null) {
            return originalValue.getAsString();
        } else {
            return mappedValue;
        }
    }


}
