/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestSecurityConfig;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.LoginStatus;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
@ContextConfiguration(classes = {TestSecurityConfig.class})
class AuthenticationSuccessTest extends BaseServiceTestRunner {

    private UserAccount person = Generator.generateUserAccount();

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
        final TermItUserDetails userDetails = new TermItUserDetails(person);
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