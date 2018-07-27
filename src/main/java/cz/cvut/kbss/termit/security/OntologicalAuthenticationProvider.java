package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.event.LoginFailureEvent;
import cz.cvut.kbss.termit.event.LoginSuccessEvent;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.service.security.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class OntologicalAuthenticationProvider implements AuthenticationProvider, ApplicationEventPublisherAware {

    private static final Logger LOG = LoggerFactory.getLogger(OntologicalAuthenticationProvider.class);

    private final SecurityUtils securityUtils;

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private ApplicationEventPublisher eventPublisher;

    @Autowired
    public OntologicalAuthenticationProvider(SecurityUtils securityUtils, UserDetailsService userDetailsService,
                                             PasswordEncoder passwordEncoder) {
        this.securityUtils = securityUtils;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getPrincipal().toString();
        verifyUsernameNotEmpty(username);
        LOG.debug("Authenticating user {}", username);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        verifyAccountStatus(userDetails.getUser());
        final String password = (String) authentication.getCredentials();
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            onLoginFailure(userDetails.getUser());
            throw new BadCredentialsException("Provided credentials don't match.");
        }
        onLoginSuccess(userDetails.getUser());
        return securityUtils.setCurrentUser(userDetails);
    }

    private void verifyUsernameNotEmpty(String username) {
        if (username.isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty.");
        }
    }

    private void verifyAccountStatus(User user) {
        if (user.isLocked()) {
            throw new LockedException("Account of user " + user + " is locked.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account of user " + user + " is disabled.");
        }
    }

    private void onLoginFailure(User user) {
        user.erasePassword();
        eventPublisher.publishEvent(new LoginFailureEvent(user));
    }

    private void onLoginSuccess(User user) {
        eventPublisher.publishEvent(new LoginSuccessEvent(user));
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass) ||
                AuthenticationToken.class.isAssignableFrom(aClass);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
