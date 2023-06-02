package com.dnastack.wes.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
@ConditionalOnExpression("${wes.workflows.authorizers.url-allow-list.enabled:false}")
public class UrlAllowListAuthorizer implements WorkflowAuthorizer {

    private final List<Predicate<String>> allowedUrlPredicates;

    public UrlAllowListAuthorizer(UrlWhiteListAuthorizerConfig config) {
        allowedUrlPredicates = config.getAllowedUrls().stream().map(Pattern::compile).map(Pattern::asMatchPredicate).toList();
    }

    @Override
    public boolean authorize(String url, MultipartFile[] contents) {
        return allowedUrlPredicates.stream().anyMatch(predicate -> predicate.test(url));
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Configuration
    @ConfigurationProperties("wes.workflows.authorizers.url-allow-list")
    public static class UrlWhiteListAuthorizerConfig {

        private List<String> allowedUrls = new ArrayList<>();

    }

}
