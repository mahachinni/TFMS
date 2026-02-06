package com.tfms.config;

import com.tfms.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()

                // Letter of Credit - Customers create/view, Officers approve/manage
                .requestMatchers("/lc/create", "/lc/submit/**").hasAnyRole("CUSTOMER", "OFFICER")
                .requestMatchers("/lc/approve/**", "/lc/reject/**", "/lc/amend/**").hasRole("OFFICER")
                .requestMatchers("/lc/**").hasAnyRole("CUSTOMER", "OFFICER","RISK")
                
                // Bank Guarantee - Customers request, Officers issue/manage
                .requestMatchers("/guarantee/request", "/guarantee/submit/**").hasAnyRole("CUSTOMER", "OFFICER")
                .requestMatchers("/guarantee/issue/**", "/guarantee/approve/**").hasRole("OFFICER")
                .requestMatchers("/guarantee/**").hasAnyRole("CUSTOMER", "OFFICER")
                
                // Trade Documents - Customers upload, Officers review
                .requestMatchers("/documents/upload", "/documents/update/**").hasAnyRole("CUSTOMER", "OFFICER")
                .requestMatchers("/documents/approve/**", "/documents/reject/**").hasAnyRole("OFFICER","RISK")
                .requestMatchers("/documents/**").hasAnyRole("CUSTOMER", "OFFICER","RISK")
                
                // Risk Assessment - Only Risk Analysts and Officers
                .requestMatchers("/risk/**").hasAnyRole("RISK", "OFFICER")
                
                // Compliance - Only Officers
                .requestMatchers("/compliance/**").hasRole("OFFICER")
                    .requestMatchers("/tracking/**").permitAll()
                
                // Dashboard and common pages
                .requestMatchers("/dashboard/**", "/").authenticated()
                .requestMatchers("/track/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );
        
        return http.build();
    }
}
