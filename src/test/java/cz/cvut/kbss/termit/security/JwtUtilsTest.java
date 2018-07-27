package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.exception.IncompleteJwtException;
import cz.cvut.kbss.termit.exception.JwtException;
import cz.cvut.kbss.termit.exception.TokenExpiredException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class JwtUtilsTest {

    private static final List<String> ROLES = Arrays.asList("USER", "ADMIN");

    @Autowired
    private Configuration config;

    private User user;

    private JwtUtils sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.user = Generator.generateUser();
        user.setUri(Generator.generateUri());
        this.sut = new JwtUtils(config);
    }

    @Test
    void generateTokenCreatesJwtForUserWithoutAuthorities() {
        final UserDetails userDetails = new UserDetails(user);
        final String jwtToken = sut.generateToken(userDetails);
        verifyJWToken(jwtToken, userDetails);
    }

    private void verifyJWToken(String token, UserDetails userDetails) {
        final Claims claims = Jwts.parser().setSigningKey(config.get(ConfigParam.JWT_SECRET_KEY)).parseClaimsJws(token)
                                  .getBody();
        assertEquals(user.getUsername(), claims.getSubject());
        assertEquals(user.getUri().toString(), claims.getId());
        assertThat(claims.getExpiration(), greaterThan(claims.getIssuedAt()));
        if (!userDetails.getAuthorities().isEmpty()) {
            assertTrue(claims.containsKey(SecurityConstants.JWT_ROLE_CLAIM));
            final String[] roles = claims.get(SecurityConstants.JWT_ROLE_CLAIM, String.class)
                                         .split(SecurityConstants.JWT_ROLE_DELIMITER);
            for (String role : roles) {
                assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority(role)));
            }
        }
    }

    @Test
    void generateTokenCreatesJwtForUserWithAuthorities() {
        final Set<GrantedAuthority> authorities = ROLES.stream().map(SimpleGrantedAuthority::new)
                                                       .collect(Collectors.toSet());
        final UserDetails userDetails = new UserDetails(user, authorities);
        final String jwtToken = sut.generateToken(userDetails);
        verifyJWToken(jwtToken, userDetails);
    }

    @Test
    void extractUserInfoExtractsDataOfUserWithoutAuthoritiesFromJWT() {
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId(user.getUri().toString())
                                 .setIssuedAt(new Date())
                                 .setExpiration(
                                         new Date(System.currentTimeMillis() + SecurityConstants.SESSION_TIMEOUT))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();

        final UserDetails result = sut.extractUserInfo(token);
        assertEquals(user, result.getUser());
        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    void extractUserInfoExtractsDataOfUserWithAuthoritiesFromJWT() {
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId(user.getUri().toString())
                                 .setIssuedAt(new Date())
                                 .setExpiration(
                                         new Date(System.currentTimeMillis() + SecurityConstants.SESSION_TIMEOUT))
                                 .claim(SecurityConstants.JWT_ROLE_CLAIM,
                                         String.join(SecurityConstants.JWT_ROLE_DELIMITER, ROLES))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();

        final UserDetails result = sut.extractUserInfo(token);
        ROLES.forEach(r -> assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority(r))));
    }

    @Test
    void extractUserInfoThrowsJwtExceptionWhenTokenCannotBeParsed() {
        final String token = "bblablalbla";
        final JwtException ex = assertThrows(JwtException.class, () -> sut.extractUserInfo(token));
        assertThat(ex.getMessage(), containsString("Unable to parse the specified JWT."));
    }

    @Test
    void extractUserInfoThrowsJwtExceptionWhenUserIdentifierIsNotValidUri() {
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId("_:123")
                                 .setIssuedAt(new Date())
                                 .setExpiration(
                                         new Date(System.currentTimeMillis() + SecurityConstants.SESSION_TIMEOUT))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        assertThrows(JwtException.class, () -> sut.extractUserInfo(token));
    }

    @Test
    void extractUserInfoThrowsIncompleteJwtExceptionWhenUsernameIsMissing() {
        final String token = Jwts.builder().setId(user.getUri().toString())
                                 .setIssuedAt(new Date())
                                 .setExpiration(
                                         new Date(System.currentTimeMillis() + SecurityConstants.SESSION_TIMEOUT))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        final IncompleteJwtException ex = assertThrows(IncompleteJwtException.class, () -> sut.extractUserInfo(token));
        assertThat(ex.getMessage(), containsString("subject"));
    }

    @Test
    void extractUserInfoThrowsIncompleteJwtExceptionWhenIdentifierIsMissing() {
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setIssuedAt(new Date())
                                 .setExpiration(
                                         new Date(System.currentTimeMillis() + SecurityConstants.SESSION_TIMEOUT))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        final IncompleteJwtException ex = assertThrows(IncompleteJwtException.class, () -> sut.extractUserInfo(token));
        assertThat(ex.getMessage(), containsString("id"));
    }

    @Test
    void extractUserInfoThrowsTokenExpiredExceptionWhenExpirationIsInPast() {
        final String token = Jwts.builder().setId(user.getUri().toString())
                                 .setSubject(user.getUsername())
                                 .setIssuedAt(new Date())
                                 .setExpiration(new Date(System.currentTimeMillis() - 1000))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        assertThrows(TokenExpiredException.class, () -> sut.extractUserInfo(token));
    }

    @Test
    void extractUserInfoThrowsTokenExpiredExceptionWhenExpirationIsMissing() {
        final String token = Jwts.builder().setId(user.getUri().toString())
                                 .setSubject(user.getUsername())
                                 .setIssuedAt(new Date())
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();
        assertThrows(TokenExpiredException.class, () -> sut.extractUserInfo(token));
    }

    @Test
    void refreshTokenUpdatesIssuedDate() {
        final Date oldIssueDate = new Date(System.currentTimeMillis() - 10000);
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId(user.getUri().toString())
                                 .setIssuedAt(oldIssueDate)
                                 .setExpiration(new Date(oldIssueDate.getTime() + SecurityConstants.SESSION_TIMEOUT))
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();

        final String result = sut.refreshToken(token);
        final Claims claims = Jwts.parser().setSigningKey(config.get(ConfigParam.JWT_SECRET_KEY)).parseClaimsJws(result)
                                  .getBody();
        assertTrue(claims.getIssuedAt().after(oldIssueDate));
    }

    @Test
    void refreshTokenUpdatesExpirationDate() {
        final Date oldIssueDate = new Date();
        final Date oldExpiration = new Date(oldIssueDate.getTime() + 10000);
        final String token = Jwts.builder().setSubject(user.getUsername())
                                 .setId(user.getUri().toString())
                                 .setIssuedAt(oldIssueDate)
                                 .setExpiration(oldExpiration)
                                 .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY)).compact();

        final String result = sut.refreshToken(token);
        final Claims claims = Jwts.parser().setSigningKey(config.get(ConfigParam.JWT_SECRET_KEY)).parseClaimsJws(result)
                                  .getBody();
        assertTrue(claims.getExpiration().after(oldExpiration));
    }
}