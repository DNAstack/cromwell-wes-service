package com.dnastack.wes.security;


import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
@ConditionalOnExpression("'${wes.auth.method}' == 'BASIC_AUTH'")
public class BasicAuthSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(AuthConfig config) {
        return username -> config.getBasicAuth().getUsers().stream().filter(user -> username.equals(user.getUsername()))
            .findAny().orElseThrow(() -> new UsernameNotFoundException("Username %s could not be found".formatted(username)));
    }

    @Bean
    public PasswordEncoder passwordEncoder(AuthConfig config) {
        if (config.getBasicAuth().isBcrypted()) {
            return new BCryptPasswordEncoder();
        } else {
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    return rawPassword.toString();
                }

                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return Objects.equals(rawPassword.toString(), encodedPassword);
                }
            };
        }
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors()
            .disable()
            .csrf()
            .disable()
            .authorizeRequests()
            .antMatchers("/services", "/actuator/info**", "/actuator/health", "/actuator/health/**", "/service-info", "/docs/**")
            .permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info")
            .permitAll()
            .antMatchers("/actuator/**")
            .authenticated()
            .antMatchers("/**")
            .authenticated()
            .and()
            .httpBasic();
        return http.build();
    }

}
