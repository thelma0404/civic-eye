package com.civic.config;

import com.civic.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Security setup.
 * <ul>
 *   <li>Passwords are hashed with BCrypt.</li>
 *   <li>Login state is kept in the HTTP session (userId + role).</li>
 *   <li>{@link RoleInterceptor} protects /citizen, /worker and /admin pages and their /api/... endpoints,
 *       redirecting pages to the right login screen and answering APIs with 401.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor())
                .addPathPatterns("/citizen/**", "/worker/**", "/admin/**",
                        "/api/citizen/**", "/api/worker/**", "/api/admin/**")
                .excludePathPatterns("/worker/login", "/admin/login", "/api/admin/auth/**");
    }

    /** Serves uploaded photos from the uploads folder at /uploads/**. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    static class RoleInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            String required;
            if (path.startsWith("/citizen") || path.startsWith("/api/citizen")) {
                required = "CITIZEN";
            } else if (path.startsWith("/worker") || path.startsWith("/api/worker")) {
                required = "WORKER";
            } else {
                required = "ADMIN";
            }
            HttpSession session = request.getSession(false);
            Object role = session == null ? null : session.getAttribute(AuthService.SESSION_ROLE);
            if (required.equals(role)) {
                return true;
            }
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Please log in to continue\",\"data\":null}");
                return false;
            }
            String login = switch (required) {
                case "CITIZEN" -> "/login";
                case "WORKER" -> "/worker/login";
                default -> "/admin/login";
            };
            response.sendRedirect(request.getContextPath() + login);
            return false;
        }
    }
}
