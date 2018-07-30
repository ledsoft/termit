package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.security.*;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.service.security.UserDetailsService;
import cz.cvut.kbss.termit.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
@ComponentScan(basePackageClasses = Security.class)
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final AuthenticationProvider authenticationProvider;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final AuthenticationSuccess authenticationSuccessHandler;

    private final AuthenticationFailureHandler authenticationFailureHandler;

    private final JwtUtils jwtUtils;

    private final SecurityUtils securityUtils;

    private final UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(AuthenticationProvider authenticationProvider,
                          AuthenticationEntryPoint authenticationEntryPoint,
                          AuthenticationSuccess authenticationSuccessHandler,
                          AuthenticationFailureHandler authenticationFailureHandler,
                          JwtUtils jwtUtils, SecurityUtils securityUtils,
                          UserDetailsService userDetailsService) {
        this.authenticationProvider = authenticationProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.jwtUtils = jwtUtils;
        this.securityUtils = securityUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().permitAll().and()
            .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
            .and().cors().and().csrf().disable()
            .addFilter(new JwtAuthenticationFilter(jwtUtils))
            .addFilter(new JwtAuthorizationFilter(authenticationManager(), jwtUtils, securityUtils, userDetailsService))
            .formLogin().successHandler(authenticationSuccessHandler)
            .failureHandler(authenticationFailureHandler)
            .loginProcessingUrl(SecurityConstants.SECURITY_CHECK_URI)
            .usernameParameter(SecurityConstants.USERNAME_PARAM).passwordParameter(SecurityConstants.PASSWORD_PARAM)
            .and().logout().logoutUrl(SecurityConstants.LOGOUT_URI).logoutSuccessHandler(authenticationSuccessHandler)
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        // We're allowing all methods from all origins so that the application API is usable also by other clients
        // than just the UI.
        // This behavior can be restricted later.
        final CorsConfiguration corsConfiguration = new CorsConfiguration().applyPermitDefaultValues();
        corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
        corsConfiguration.setAllowedOrigins(Collections.singletonList("*"));
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(Constants.REST_MAPPING_PATH + "/**", corsConfiguration);
        return source;
    }
}
