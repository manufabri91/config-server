package com.manuelfabri.configserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class CryptoEndpointSecurity {

  private static final String[] CRYPTO_PATHS = {"/encrypt", "/encrypt/**", "/decrypt", "/decrypt/**"};

  @Bean
  UserDetailsService cryptoUsers(@Value("${config.crypto.username}") String username,
      @Value("${config.crypto.password}") String password) {
    if (password.isBlank()) {
      return new InMemoryUserDetailsManager();
    }
    String encodedPassword = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
    return new InMemoryUserDetailsManager(User.withUsername(username).password(encodedPassword)
        .roles("CONFIG_CRYPTO").build());
  }

  @Bean
  SecurityFilterChain configSecurity(HttpSecurity http, @Value("${config.crypto.password}") String password)
      throws Exception {
    http.csrf(csrf -> csrf.ignoringRequestMatchers(CRYPTO_PATHS));
    if (password.isBlank()) {
      http.authorizeHttpRequests(auth -> auth.requestMatchers(CRYPTO_PATHS).denyAll().anyRequest().permitAll())
          .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)));
    } else {
      http.authorizeHttpRequests(auth -> auth.requestMatchers(CRYPTO_PATHS).hasRole("CONFIG_CRYPTO")
          .anyRequest().permitAll()).httpBasic(Customizer.withDefaults());
    }
    return http.build();
  }
}
