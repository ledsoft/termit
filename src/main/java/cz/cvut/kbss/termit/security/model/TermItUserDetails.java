package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.model.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

public class TermItUserDetails implements UserDetails {

    /**
     * Default authority held by all registered users of the system.
     */
    public static final GrantedAuthority DEFAULT_AUTHORITY = new SimpleGrantedAuthority(UserRole.USER.getName());

    private UserAccount user;

    private final Set<GrantedAuthority> authorities;

    public TermItUserDetails(UserAccount user) {
        Objects.requireNonNull(user);
        this.user = user;
        this.authorities = resolveAuthorities(user);
    }

    public TermItUserDetails(UserAccount user, Collection<GrantedAuthority> authorities) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(authorities);
        this.user = user;
        this.authorities = resolveAuthorities(user);
        this.authorities.addAll(authorities);
    }

    private static Set<GrantedAuthority> resolveAuthorities(UserAccount user) {
        final Set<GrantedAuthority> authorities = new HashSet<>(4);
        authorities.add(DEFAULT_AUTHORITY);
        if (user.getTypes() != null) {
            authorities.addAll(user.getTypes().stream().filter(UserRole::exists)
                                   .map(r -> new SimpleGrantedAuthority(UserRole.fromType(r).getName()))
                                   .collect(Collectors.toSet()));
        }
        return authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableCollection(authorities);
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    public UserAccount getUser() {
        return user.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TermItUserDetails)) {
            return false;
        }
        TermItUserDetails that = (TermItUserDetails) o;
        return Objects.equals(user, that.user) && Objects.equals(authorities, that.authorities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, authorities);
    }

    @Override
    public String toString() {
        return "UserDetails{" +
                "user=" + user +
                ", authorities=" + authorities +
                '}';
    }
}
