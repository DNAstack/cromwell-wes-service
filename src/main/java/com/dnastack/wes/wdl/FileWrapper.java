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
    @Getter
    String mappedValue;

    @Setter
    @Getter
    Boolean wasMapped = false;

    @Setter
    @Getter
    Boolean requiresTransfer = false;

    public FileWrapper(JsonElement original) {
        this.original = original;
        this.mappedValue = original.getAsString();
    }
}
