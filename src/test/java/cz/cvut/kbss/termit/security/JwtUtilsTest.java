package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class JwtUtilsTest {

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
        final Date expires = claims.getExpiration();
        assertThat(expires.compareTo(new Date(claims.getIssuedAt().getTime() + SecurityConstants.SESSION_TIMEOUT)),
                greaterThanOrEqualTo(0));
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
        final List<String> roles = Arrays.asList("USER", "ADMIN");
        final Set<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
                                                       .collect(Collectors.toSet());
        final UserDetails userDetails = new UserDetails(user, authorities);
        final String jwtToken = sut.generateToken(userDetails);
        verifyJWToken(jwtToken, userDetails);
    }
}