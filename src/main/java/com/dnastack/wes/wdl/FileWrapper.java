package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = FileWrapperSerializer.class)
public class FileWrapper {

    @Getter
    JsonElement original;

    @Setter
    String mappedValue;

    @Setter
    @Getter
    Boolean wasMapped = false;

    @Setter
    @Getter
    Boolean requiresTransfer = false;

    public FileWrapper(JsonElement original) {
        this.original = original;
    }

    public String getMappedValue() {
        if (mappedValue == null) {
            return original.toString();
        } else {
            return mappedValue;
        }
    }

}
