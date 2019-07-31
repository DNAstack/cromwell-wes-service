package com.dnastack.wes.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {


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
            .oauth2Login().and()
            .oauth2ResourceServer()
            .jwt();
    }


}
