package vn.vnpost.lunchorder.system.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.vnpost.lunchorder.system.security.jwt.JwtAuthenticationEntryPoint;
import vn.vnpost.lunchorder.system.security.jwt.JwtAuthenticationFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // No pre-authenticated state to protect: login/refresh/logout need no prior
                        // XSRF cookie, introspect is called service-to-service (no browser cookies involved).
                        .ignoringRequestMatchers("/auth/login", "/auth/introspect", "/auth/refresh", "/auth/logout"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Personal profile endpoint must stay behind authentication,
                        // so match it before the public /auth/** rule below.
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/me/change-password").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated())
                // Unauthenticated → 401 (session expired), authenticated-but-denied → 403 (default handler)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, CsrfFilter.class)
                // Forces the deferred CSRF token to be written to the response cookie on every request,
                // not just when a form/template reads it (CookieCsrfTokenRepository is lazy otherwise).
                .addFilterAfter(csrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // Without this the cookie defaults to Path=/api/v1 (the context path), which the SPA
        // at localhost:4200/ cannot read via document.cookie, so it can never echo the header.
        repository.setCookiePath("/");
        // Match the JWT cookie's lifetime so the XSRF cookie survives a browser restart
        // (default is a session cookie, which would break the silent-refresh-on-load flow).
        repository.setCookieCustomizer(cookie -> cookie.maxAge(refreshableDuration));
        return repository;
    }

    private OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Cache-Control", "Accept", "Origin", "X-Requested-With",
                        "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
