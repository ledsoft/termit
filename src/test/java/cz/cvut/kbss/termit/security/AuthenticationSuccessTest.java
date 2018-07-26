package cz.cvut.kbss.termit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestSecurityConfig;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.LoginStatus;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(classes = {TestSecurityConfig.class})
class AuthenticationSuccessTest extends BaseServiceTestRunner {

    private User person = Generator.generateUser();

    @Autowired
    private AuthenticationSuccess success;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void authenticationSuccessReturnsResponseContainingUsername() throws Exception {
        final MockHttpServletResponse response = response();
        success.onAuthenticationSuccess(request(), response, generateAuthenticationToken());
        verifyLoginStatus(response);
    }

    private void verifyLoginStatus(MockHttpServletResponse response) throws java.io.IOException {
        final LoginStatus status = mapper.readValue(response.getContentAsString(), LoginStatus.class);
        assertTrue(status.isSuccess());
        assertTrue(status.isLoggedIn());
        assertEquals(person.getUsername(), status.getUsername());
        assertNull(status.getErrorMessage());
    }

    static MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    static MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private Authentication generateAuthenticationToken() {
        final UserDetails userDetails = new UserDetails(person);
        return new AuthenticationToken(userDetails.getAuthorities(), userDetails);
    }

    @Test
    void logoutSuccessReturnsResponseContainingLoginStatus() throws Exception {
        final MockHttpServletResponse response = response();
        success.onLogoutSuccess(request(), response, generateAuthenticationToken());
        final LoginStatus status = mapper.readValue(response.getContentAsString(), LoginStatus.class);
        assertTrue(status.isSuccess());
        assertFalse(status.isLoggedIn());
        assertNull(status.getUsername());
        assertNull(status.getErrorMessage());
    }
}