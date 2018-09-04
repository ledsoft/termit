package cz.cvut.kbss.termit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.service.security.UserDetailsService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.FilterChain;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("security")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class JwtAuthorizationFilterTest {

    @Autowired
    private Configuration config;

    private User user;

    private MockHttpServletRequest mockRequest = new MockHttpServletRequest();

    private MockHttpServletResponse mockResponse = new MockHttpServletResponse();

    @Mock
    private FilterChain chainMock;

    @Mock
    private AuthenticationManager authManagerMock;

    @Mock
    private UserDetailsService detailsServiceMock;

    @Mock
    private SecurityUtils securityUtilsMock;

    private JwtUtils jwtUtilsSpy;

    private ObjectMapper objectMapper;

    private JwtAuthorizationFilter sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.user = Generator.generateUserWithId();
        this.jwtUtilsSpy = spy(new JwtUtils(config));
        this.objectMapper = Environment.getObjectMapper();
        this.sut = new JwtAuthorizationFilter(authManagerMock, jwtUtilsSpy, securityUtilsMock, detailsServiceMock,
                objectMapper);
        when(detailsServiceMock.loadUserByUsername(user.getUsername())).thenReturn(new UserDetails(user));
    }

    @Test
    void doFilterInternalExtractsUserInfoFromJwtAndSetsUpSecurityContext() throws Exception {
        generateJwtIntoRequest();

        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        final ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(securityUtilsMock).setCurrentUser(captor.capture());
        final UserDetails userDetails = captor.getValue();
        assertEquals(user, userDetails.getUser());
    }

    private void generateJwtIntoRequest() {
        final String token = generateJwt();
        mockRequest.addHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + token);
    }

    private String generateJwt() {
        return Jwts.builder().setSubject(user.getUsername())
                   .setId(user.getUri().toString())
                   .setIssuedAt(new Date())
                   .setExpiration(new Date(System.currentTimeMillis() + 10000))
                   .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
    }

    @Test
    void doFilterInternalInvokesFilterChainAfterSuccessfulExtractionOfUserInfo() throws Exception {
        generateJwtIntoRequest();
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        verify(chainMock).doFilter(mockRequest, mockResponse);
    }

    @Test
    void doFilterInternalLeavesEmptySecurityContextAndPassesRequestDownChainWhenAuthenticationIsMissing()
            throws Exception {
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        verify(chainMock).doFilter(mockRequest, mockResponse);
        verify(securityUtilsMock, never()).setCurrentUser(any());
    }

    @Test
    void doFilterInternalLeavesEmptySecurityContextAndPassesRequestDownChainWhenAuthenticationHasIncorrectFormat()
            throws Exception {
        mockRequest.addHeader(SecurityConstants.AUTHENTICATION_HEADER, generateJwt());
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        verify(chainMock).doFilter(mockRequest, mockResponse);
        verify(securityUtilsMock, never()).setCurrentUser(any());
    }

    @Test
    void doFilterInternalRefreshesUserTokenOnSuccessfulAuthorization() throws Exception {
        generateJwtIntoRequest();
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertTrue(mockResponse.containsHeader(SecurityConstants.AUTHENTICATION_HEADER));
        assertNotEquals(mockRequest.getHeader(SecurityConstants.AUTHENTICATION_HEADER),
                mockResponse.getHeader(SecurityConstants.AUTHENTICATION_HEADER));
        verify(jwtUtilsSpy).refreshToken(any());
    }

    @Test
    void doFilterInternalReturnsUnauthorizedWhenWhenTokenIsExpired() throws Exception {
        final String token = Jwts.builder().setSubject(user.getUsername())
            .setId(user.getUri().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() - 10000))
            .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        mockRequest.addHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + token);
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
        final ErrorInfo errorInfo = objectMapper.readValue(mockResponse.getContentAsString(), ErrorInfo.class);
        assertNotNull(errorInfo);
        assertThat(errorInfo.getMessage(), containsString("expired"));
    }

    @Test
    void doFilterInternalReturnsUnauthorizedWhenUserAccountIsLocked() throws Exception {
        generateJwtIntoRequest();
        user.lock();
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
        final ErrorInfo errorInfo = objectMapper.readValue(mockResponse.getContentAsString(), ErrorInfo.class);
        assertNotNull(errorInfo);
        assertThat(errorInfo.getMessage(), containsString("locked"));
    }

    @Test
    void doFilterInternalReturnsUnauthorizedWhenUserAccountIsDisabled() throws Exception {
        generateJwtIntoRequest();
        user.disable();
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
        final ErrorInfo errorInfo = objectMapper.readValue(mockResponse.getContentAsString(), ErrorInfo.class);
        assertNotNull(errorInfo);
        assertThat(errorInfo.getMessage(), containsString("disabled"));
    }

    @Test
    void doFilterInternalReturnsBadRequestOnIncompleteJwtToken() throws Exception {
        // Missing id
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setIssuedAt(new Date())
                                 .setExpiration(new Date(System.currentTimeMillis() + 10000))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        mockRequest.addHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + token);
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mockResponse.getStatus());
        final ErrorInfo errorInfo = objectMapper.readValue(mockResponse.getContentAsString(), ErrorInfo.class);
        assertNotNull(errorInfo);
        assertThat(errorInfo.getMessage(), containsString("missing"));
    }

    @Test
    void doFilterInternalReturnsBadRequestOnUnparseableUserInfoInJwtToken() throws Exception {
        // Missing id
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId(":1235")    // Not valid URI
                                 .setIssuedAt(new Date())
                                 .setExpiration(new Date(System.currentTimeMillis() + 10000))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        mockRequest.addHeader(SecurityConstants.AUTHENTICATION_HEADER, SecurityConstants.JWT_TOKEN_PREFIX + token);
        sut.doFilterInternal(mockRequest, mockResponse, chainMock);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mockResponse.getStatus());
        final ErrorInfo errorInfo = objectMapper.readValue(mockResponse.getContentAsString(), ErrorInfo.class);
        assertNotNull(errorInfo);
    }
}