package com.dnastack.wes.config;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PathTranslationConfig {

    String prefix;
    String replacement;
    PathLocation location = PathLocation.ALL;

    public enum PathLocation {
        ALL, INPUTS, OUTPUTS
    }

}
