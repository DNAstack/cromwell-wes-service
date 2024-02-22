package com.dnastack.wes.security;


import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

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
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(requestConfig -> requestConfig.requestMatchers(
                    antMatcher("/services"),
                    antMatcher("/actuator/info**"),
                    antMatcher("/actuator/health"),
                    antMatcher("/actuator/health/**"),
                    antMatcher("/service-info"),
                    antMatcher("/docs/**"),
                    antMatcher("/ga4gh/wes/v1/service-info")
                ).permitAll()
                .requestMatchers(
                    antMatcher("/actuator/**"),
                    antMatcher("/**")
                ).authenticated())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

}
