package cz.cvut.kbss.termit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.config.TestSecurityConfig;
import cz.cvut.kbss.termit.security.model.LoginStatus;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
@ContextConfiguration(classes = {TestSecurityConfig.class})
class AuthenticationFailureTest extends BaseServiceTestRunner {

    @Autowired
    private AuthenticationFailure failure;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void authenticationFailureReturnsLoginStatusWithErrorInfoOnUsernameNotFound() throws Exception {
        final MockHttpServletRequest request = AuthenticationSuccessTest.request();
        final MockHttpServletResponse response = AuthenticationSuccessTest.response();
        final String msg = "Username not found";
        final AuthenticationException e = new UsernameNotFoundException(msg);
        failure.onAuthenticationFailure(request, response, e);
        final LoginStatus status = mapper.readValue(response.getContentAsString(), LoginStatus.class);
        assertFalse(status.isSuccess());
        assertFalse(status.isLoggedIn());
        assertNull(status.getUsername());
        assertEquals(msg, status.getErrorMessage());
        assertEquals("login.error", status.getErrorId());
    }

    @Test
    void authenticationFailureReturnsLoginStatusWithErrorInfoOnAccountLocked() throws Exception {
        final MockHttpServletRequest request = AuthenticationSuccessTest.request();
        final MockHttpServletResponse response = AuthenticationSuccessTest.response();
        final String msg = "Account is locked.";
        failure.onAuthenticationFailure(request, response, new LockedException(msg));
        final LoginStatus status = mapper.readValue(response.getContentAsString(), LoginStatus.class);
        assertFalse(status.isSuccess());
        assertFalse(status.isLoggedIn());
        assertNull(status.getUsername());
        assertEquals(msg, status.getErrorMessage());
        assertEquals("login.locked", status.getErrorId());
    }

    @Test
    void authenticationFailureReturnsLoginStatusWithErrorInfoOnAccountDisabled() throws Exception {
        final MockHttpServletRequest request = AuthenticationSuccessTest.request();
        final MockHttpServletResponse response = AuthenticationSuccessTest.response();
        final String msg = "Account is disabled.";
        failure.onAuthenticationFailure(request, response, new DisabledException(msg));
        final LoginStatus status = mapper.readValue(response.getContentAsString(), LoginStatus.class);
        assertFalse(status.isSuccess());
        assertFalse(status.isLoggedIn());
        assertNull(status.getUsername());
        assertEquals(msg, status.getErrorMessage());
        assertEquals("login.disabled", status.getErrorId());
    }
}