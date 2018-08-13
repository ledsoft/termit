package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.FilterChain;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Tag("security")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class JwtAuthenticationFilterTest {

    @Autowired
    private Configuration config;

    private MockHttpServletRequest mockRequest;

    private MockHttpServletResponse mockResponse;

    private User user;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.user = Generator.generateUserWithId();
        this.mockRequest = new MockHttpServletRequest();
        this.mockResponse = new MockHttpServletResponse();
        this.sut = new JwtAuthenticationFilter(mock(AuthenticationManager.class), new JwtUtils(config));
    }

    @Test
    void successfulAuthenticationExposesAuthenticationHeaderToClient() throws Exception {
        final AuthenticationToken token = new AuthenticationToken(Collections.emptySet(), new UserDetails(user));
        sut.successfulAuthentication(mockRequest, mockResponse, filterChain, token);
        assertTrue(mockResponse.containsHeader(SecurityConstants.AUTHENTICATION_HEADER));
        final String value = mockResponse.getHeader(SecurityConstants.EXPOSE_HEADERS_HEADER);
        assertNotNull(value);
        assertThat(value, containsString(SecurityConstants.AUTHENTICATION_HEADER));
    }

    @Test
    void successfulAuthenticationAddsJWTToResponse() throws Exception {
        final AuthenticationToken token = new AuthenticationToken(Collections.emptySet(), new UserDetails(user));
        sut.successfulAuthentication(mockRequest, mockResponse, filterChain, token);
        assertTrue(mockResponse.containsHeader(SecurityConstants.AUTHENTICATION_HEADER));
        final String value = mockResponse.getHeader(SecurityConstants.AUTHENTICATION_HEADER);
        assertNotNull(value);
        assertTrue(value.startsWith(SecurityConstants.JWT_TOKEN_PREFIX));
        final String jwtToken = value.substring(SecurityConstants.JWT_TOKEN_PREFIX.length());
        final Jws<Claims> jwt = Jwts.parser().setSigningKey(config.get(ConfigParam.JWT_SECRET_KEY))
                                    .parseClaimsJws(jwtToken);
        assertFalse(jwt.getBody().isEmpty());
    }
}