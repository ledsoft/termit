package cz.cvut.kbss.termit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.exception.JwtException;
import cz.cvut.kbss.termit.exception.TokenExpiredException;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.service.security.UserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter retrieves JWT from the incoming request and validates it, ensuring that the user is authorized to access
 * the application.
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtUtils jwtUtils;

    private final SecurityUtils securityUtils;

    private final UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                                  SecurityUtils securityUtils, UserDetailsService userDetailsService,
                                  ObjectMapper objectMapper) {
        super(authenticationManager);
        this.jwtUtils = jwtUtils;
        this.securityUtils = securityUtils;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String authHeader = request.getHeader(SecurityConstants.AUTHENTICATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.JWT_TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        final String authToken = authHeader.substring(SecurityConstants.JWT_TOKEN_PREFIX.length());
        try {
            final UserDetails userDetails = jwtUtils.extractUserInfo(authToken);
            final UserDetails existingDetails = userDetailsService.loadUserByUsername(userDetails.getUsername());
            SecurityUtils.verifyAccountStatus(existingDetails.getUser());
            securityUtils.setCurrentUser(existingDetails);
            refreshToken(authToken, response);
        } catch (DisabledException | LockedException | TokenExpiredException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            objectMapper.writeValue(response.getOutputStream(), new ErrorInfo(e.getMessage(), request.getRequestURI()));
            return;
        } catch (JwtException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getOutputStream(), new ErrorInfo(e.getMessage(), request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private void refreshToken(String authToken, HttpServletResponse response) {
        final String newToken = jwtUtils.refreshToken(authToken);
        response.setHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + newToken);
    }
}
