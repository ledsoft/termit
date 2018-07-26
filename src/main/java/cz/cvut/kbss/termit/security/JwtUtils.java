package cz.cvut.kbss.termit.security;

import cz.cvut.kbss.termit.security.model.UserDetails;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
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
        return null;
    }
}
