package com.dnastack.wes.wdl;

import com.dnastack.wes.config.AppConfig;
import com.dnastack.wes.config.PathTranslationConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

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

                pathtranslators.add(new PathTranslator(prefix, replacement));
            }
        }
        translators = Collections.unmodifiableList(pathtranslators);
    }

    public List<PathTranslator> getTranslators() {
        return translators;
    }

}
