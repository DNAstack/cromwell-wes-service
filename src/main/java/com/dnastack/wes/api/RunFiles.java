package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@Builder
public class RunFiles {

    @JsonProperty("files")
    List<RunFile> files;

    public void addRunFile(RunFile runFile) {
        if (files == null) {
            files = new ArrayList<>();
        }
        files.add(runFile);
    }

}
