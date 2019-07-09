package com.dnastack.wes.security;

import com.dnastack.wes.AuthConfig;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private JwtAuthenticationConverter jwtAuthenticationConverter;


    @Autowired
    public SecurityConfiguration(Map<String, JwtAuthenticationConverter> converterMap, AuthConfig authConfig) {
        this.jwtAuthenticationConverter = converterMap.get(authConfig.getIdentityProvider());
        if (this.jwtAuthenticationConverter == null) {
            throw new IllegalArgumentException(
                "Unrecognized identity provider " + authConfig.getIdentityProvider());
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/actuator", "/actuator/info", "/actuator/health").permitAll()
            .antMatchers("/").permitAll()
            .antMatchers("/index.html").permitAll()
            .antMatchers("/ga4gh/drs/**").permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt().jwtAuthenticationConverter(jwtAuthenticationConverter);
    }

    @Bean
    public OAuth2RestTemplate oauth2RestTemplate(OAuth2ProtectedResourceDetails details) {
        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(details);

        // Validate everything is working on startup.
        try {
            OAuth2AccessToken test = oAuth2RestTemplate.getAccessToken();
            //log.debug("Got OAuth access token for service account ending with " + test.getValue()
            //																		  .substring(test.getValue().length() - 5));
            log.debug("Got OAuth access token for service account " + test.getValue());
            return oAuth2RestTemplate;
        } catch (OAuth2AccessDeniedException ex) {
            log.error("Couldn't obtain access token for service account -- check configuration properties (security.oauth2.client.*)");
            log.error("Got oauth2 error code " + ex.getOAuth2ErrorCode());
            log.debug("ClientId: " + ex.getResource().getClientId());
            log.debug("Endpoint: " + ex.getResource().getAccessTokenUri());
            throw ex;
        }
    }

}
