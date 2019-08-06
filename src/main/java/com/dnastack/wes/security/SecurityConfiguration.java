package com.dnastack.wes.security;

import com.dnastack.wes.config.AuthConfig;
import com.dnastack.wes.config.AuthConfig.IssuerConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Bean
    public JwtDecoder jwtDecoder(AuthConfig authConfig) {
        List<IssuerConfig> issuers = authConfig.getTokenIssuers();
        IssuerConfig devTokenIssuer = authConfig.getDevTokenIssuer();

        if (issuers == null || issuers.isEmpty()) {
            throw new IllegalArgumentException("At least one token issuer must be defined");
        }

        issuers = new ArrayList<>(issuers);
        if (devTokenIssuer != null) {
            issuers.add(devTokenIssuer);
        }

        return new DelegatingJwtDecoder(issuers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers("/actuator", "/actuator/info", "/actuator/health").permitAll()
            .antMatchers("/").permitAll()
            .antMatchers("/index.html").permitAll()
            .antMatchers("/ga4gh/drs/**").permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt();
    }


}
