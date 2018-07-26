package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.exception.IncompleteJwtException;
import cz.cvut.kbss.termit.exception.JwtException;
import cz.cvut.kbss.termit.exception.TokenExpiredException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    private final Configuration config;

    @Autowired
    public JwtUtils(Configuration config) {
        this.config = config;
    }

    /**
     * Generates a JSON Web Token for the specified authenticated user.
     *
     * @param userDetails User info
     * @return Generated JWT has
     */
    public String generateToken(UserDetails userDetails) {
        final Date issued = new Date();
        return Jwts.builder().setSubject(userDetails.getUsername())
                   .setId(userDetails.getUser().getUri().toString())
                   .setIssuedAt(issued)
                   .setExpiration(new Date(issued.getTime() + SecurityConstants.SESSION_TIMEOUT))
                   .claim(SecurityConstants.JWT_ROLE_CLAIM, mapAuthoritiesToClaim(userDetails.getAuthorities()))
                   .signWith(SignatureAlgorithm.HS512, config.get(ConfigParam.JWT_SECRET_KEY))
                   .compact();
    }

    private String mapAuthoritiesToClaim(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority)
                          .collect(Collectors.joining(SecurityConstants.JWT_ROLE_DELIMITER));
    }

    /**
     * Retrieves user info from the specified JWT.
     * <p>
     * The token is first validated for correct format and expiration date.
     *
     * @param token JWT to read
     * @return User info retrieved from the specified token
     */
    public UserDetails extractUserInfo(String token) {
        try {
            final Claims claims = Jwts.parser().setSigningKey(config.get(ConfigParam.JWT_SECRET_KEY))
                                      .parseClaimsJws(token).getBody();
            verifyAttributePresence(claims);
            final User user = new User();
            user.setUri(URI.create(claims.getId()));
            user.setUsername(claims.getSubject());
            final String roles = claims.get(SecurityConstants.JWT_ROLE_CLAIM, String.class);
            return new UserDetails(user, mapClaimToAuthorities(roles));
        } catch (MalformedJwtException e) {
            throw new JwtException("Unable to parse the specified JWT.", e);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new JwtException("Unable to parse user identifier from the specified JWT.");
        }
    }

    private void verifyAttributePresence(Claims claims) {
        if (claims.getSubject() == null) {
            throw new IncompleteJwtException("JWT is missing subject.");
        }
        if (claims.getId() == null) {
            throw new IncompleteJwtException("JWT is missing id.");
        }
        if (claims.getExpiration() == null) {
            throw new TokenExpiredException("Missing token expiration info. Assuming expired.");
        }
    }

    private List<GrantedAuthority> mapClaimToAuthorities(String claim) {
        if (claim == null) {
            return Collections.emptyList();
        }
        final String[] roles = claim.split(SecurityConstants.JWT_ROLE_DELIMITER);
        final List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return authorities;
    }
}
