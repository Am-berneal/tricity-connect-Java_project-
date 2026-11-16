package com.tricityconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Who can open which page.
 *  - Anyone: home, search, alerts, login, register
 *  - Logged-in passengers: seat booking, checkout, bookings, passes, tracking
 *  - ADMIN only: /admin/**      CONDUCTOR or ADMIN: /conductor/**
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Matches Spring MVC URLs; needed because the H2 console adds a second servlet. */
    @Bean
    public MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(mvc.pattern("/"), mvc.pattern("/search"), mvc.pattern("/alerts"),
                        mvc.pattern("/login"), mvc.pattern("/register"), mvc.pattern("/error"),
                        mvc.pattern("/images/**"), mvc.pattern("/css/**"), mvc.pattern("/js/**"),
                        mvc.pattern("/favicon.ico"),
                        mvc.pattern("/help"), mvc.pattern("/help/ask"), mvc.pattern("/help/chat/clear")).permitAll()
                .requestMatchers(antMatcher("/h2-console/**")).permitAll()
                .requestMatchers(mvc.pattern("/admin/**")).hasRole("ADMIN")
                .requestMatchers(mvc.pattern("/conductor/**")).hasAnyRole("CONDUCTOR", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler())
                .failureUrl("/login?error")
                .permitAll())
            .rememberMe(rm -> rm.key("tricity-connect-remember-me").tokenValiditySeconds(14 * 24 * 3600))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            // the H2 database console (only used with the h2 profile) needs these two lines
            .csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher("/h2-console/**")))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    /** After login: admins go to the admin area, conductors to their screen, passengers back to where they were. */
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler passengerHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        passengerHandler.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            var roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin");
            } else if (roles.contains("ROLE_CONDUCTOR")) {
                response.sendRedirect("/conductor");
            } else {
                passengerHandler.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }
}
