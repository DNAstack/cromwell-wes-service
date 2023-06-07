package com.dnastack.wes.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@ConditionalOnExpression("'${wes.auth.method}' == 'NO_AUTH'")
public class NoAuthSecurityConfig {

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors().disable()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/**")
            .permitAll();
        return http.build();
    }


}
