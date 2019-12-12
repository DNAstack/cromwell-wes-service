package com.dnastack.wes.service;

import com.dnastack.wes.client.ClientConfigurations.SimpleLogger;
import com.dnastack.wes.client.DrsClient;
import com.dnastack.wes.config.DrsConfig;
import feign.Client;
import feign.Feign;
import feign.Logger.Level;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DrsObjectResolverFactory {


    private final DrsConfig drsConfig;

    @Autowired
    public DrsObjectResolverFactory(DrsConfig drsConfig) {
        this.drsConfig = drsConfig;
    }

    public DrsObjectResolver getService(Map<String, String> tokens) {
        return new DrsObjectResolver(drsConfig, getClient(tokens));
    }

    private DrsClient getClient(Map<String, String> tokens) {
        Client httpClient = new OkHttpClient();
        return Feign.builder().client(httpClient).encoder(new JacksonEncoder()).decoder(new JacksonDecoder())
            .logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .requestInterceptor(template -> {
                String requestUrl = template.url();
                String token = null;
                if (tokens.containsKey(requestUrl)) {
                    token = tokens.get(requestUrl);
                } else {
                    URI uri = URI.create(requestUrl);
                    String host = "drs://" + uri.getHost();
                    String id = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);

                    String drsUri = host + "/" + id;

                    if (tokens.containsKey(drsUri)) {
                        token = tokens.get(drsUri);
                    } else if (tokens.containsKey(host)) {
                        token = tokens.get(host);
                    }

                }

                if (token != null) {
                    template.header("Authorization", "Bearer " + token);
                }
            })
            .target(Target.EmptyTarget.create(DrsClient.class));
    }

}