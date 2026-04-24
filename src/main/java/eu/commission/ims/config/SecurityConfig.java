package eu.commission.ims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration using HTTP Basic Authentication.
 *
 * <p>Roles:
 * <ul>
 *   <li>ADMIN — full CRUD access to all modules</li>
 *   <li>RECRUITER — can manage resumes and screenings</li>
 *   <li>INTERVIEWER — can manage interviews and feedback</li>
 * </ul>
 *
 * <p>Default credentials (for demo/DevSecOps showcase):
 * <ul>
 *   <li>admin / admin123</li>
 *   <li>recruiter / rec123</li>
 *   <li>interviewer / int123</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info",
            "/h2-console/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/v1/resumes/**").hasAnyRole("ADMIN", "RECRUITER")
                        .requestMatchers("/api/v1/screenings/**").hasAnyRole("ADMIN", "RECRUITER")
                        .requestMatchers("/api/v1/interviews/**").hasAnyRole("ADMIN", "INTERVIEWER")
                        .requestMatchers("/api/v1/feedback/**").hasAnyRole("ADMIN", "INTERVIEWER")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // Allow H2 console frames
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        var recruiter = User.builder()
                .username("recruiter")
                .password(passwordEncoder.encode("rec123"))
                .roles("RECRUITER")
                .build();

        var interviewer = User.builder()
                .username("interviewer")
                .password(passwordEncoder.encode("int123"))
                .roles("INTERVIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, recruiter, interviewer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
