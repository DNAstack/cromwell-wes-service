package com.dnastack.wes.api;

import com.dnastack.wes.AppConfig;
import com.dnastack.wes.api.PathTranslationConfig.PathLocation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PathTranslatorFactory {


    private final List<PathTranslator> translators;

    public PathTranslatorFactory(AppConfig config) {
        List<PathTranslationConfig> pathConfigs = config.getPathTranslations();
        List<PathTranslator> pathtranslators = new ArrayList<>();
        if (pathConfigs != null && !pathConfigs.isEmpty()) {
            for (PathTranslationConfig pathConfig : pathConfigs) {
                String prefix = pathConfig.getPrefix();
                String replacement = pathConfig.getReplacement();

                if (prefix == null || prefix.isBlank()) {
                    throw new IllegalArgumentException("Invalid path translation config, prefix is not defined");
                }

                if (replacement == null) {
                    replacement = "";
                }

                pathtranslators.add(new PathTranslator(prefix, replacement, pathConfig.getLocation()));
            }
        }
        translators = Collections.unmodifiableList(pathtranslators);
    }


    public List<PathTranslator> getTranslatorsForInputs() {
        return translators.stream()
            .filter(t -> t.getLocation().equals(PathLocation.ALL) || t.getLocation().equals(PathLocation.INPUTS))
            .collect(Collectors.toList());
    }

    public List<PathTranslator> getTranslatorsForOutputs() {
        return translators.stream()
            .filter(t -> t.getLocation().equals(PathLocation.ALL) || t.getLocation().equals(PathLocation.OUTPUTS))
            .collect(Collectors.toList());
    }

}
