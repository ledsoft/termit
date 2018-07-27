package cz.cvut.kbss.termit.security.model;

import cz.cvut.kbss.termit.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

public class UserDetails implements org.springframework.security.core.userdetails.UserDetails {

    /**
     * Default authority held by all registered users of the system.
     */
    public static final GrantedAuthority DEFAULT_AUTHORITY = new SimpleGrantedAuthority(UserRole.USER.getName());

    private User user;

    private final Set<GrantedAuthority> authorities;

    public UserDetails(User user) {
        Objects.requireNonNull(user);
        this.user = user;
        this.authorities = resolveAuthorities(user);
    }

    public UserDetails(User user, Collection<GrantedAuthority> authorities) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(authorities);
        this.user = user;
        this.authorities = resolveAuthorities(user);
        this.authorities.addAll(authorities);
    }

    private Set<GrantedAuthority> resolveAuthorities(User user) {
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

    public User getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDetails)) {
            return false;
        }
        UserDetails that = (UserDetails) o;
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
