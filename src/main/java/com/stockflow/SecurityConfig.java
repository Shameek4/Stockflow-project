package com.stockflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(
      @Value("${stockflow.admin.password}") String admin,
      @Value("${stockflow.staff.password}") String staff) {
    if (admin.length() < 12 || staff.length() < 12)
      throw new IllegalStateException(
          "Set STOCKFLOW_ADMIN_PASSWORD and STOCKFLOW_STAFF_PASSWORD (at least 12 characters"
              + " each)");
    var encoder = new BCryptPasswordEncoder();
    return new InMemoryUserDetailsManager(
        User.withUsername("admin").password(encoder.encode(admin)).roles("ADMIN").build(),
        User.withUsername("staff").password(encoder.encode(staff)).roles("STAFF").build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/login.html", "/style.css", "/login.js", "/api/csrf", "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/products",
                        "/api/products/*/stock",
                        "/api/products/*/archive")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/*")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login.html")
                    .loginProcessingUrl("/login")
                    .successHandler((req, res, a) -> res.setStatus(204))
                    .failureHandler(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter().write("{\"message\":\"Invalid username or password\"}");
                        })
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((req, res, a) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    (req, res, ex) -> {
                      res.setStatus(401);
                      res.setContentType("application/json");
                      res.getWriter().write("{\"message\":\"Please sign in\"}");
                    },
                    req -> req.getRequestURI().startsWith("/api/")))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self' data:; frame-ancestors 'none'; base-uri 'self'")));
    return http.build();
  }
}
