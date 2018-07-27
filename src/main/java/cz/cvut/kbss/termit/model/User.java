package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@OWLClass(iri = Vocabulary.s_c_user)
public class User implements Serializable {

    @Id
    private URI uri;

    @NotEmpty
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_givenName)
    private String firstName;

    @NotEmpty
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_familyName)
    private String lastName;

    @NotEmpty
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_username)
    private String username;

    @NotEmpty
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_password)
    private String password;

    @Types
    private Set<String> types;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public void addType(String type) {
        Objects.requireNonNull(type);
        if (types == null) {
            this.types = new HashSet<>(4);
        }
        types.add(type);
    }

    /**
     * Erases the password in this instance.
     * <p>
     * This should be used for security reasons when passing the instance throughout the application and especially when
     * it to be send from the REST API to the client.
     */
    public void erasePassword() {
        this.password = null;
    }

    /**
     * Checks whether the account represented by this instance is locked.
     *
     * @return Locked status
     */
    public boolean isLocked() {
        return types != null && types.contains(Vocabulary.s_c_locked_user);
    }

    /**
     * Locks the account represented by this instance.
     */
    public void lock() {
        addType(Vocabulary.s_c_locked_user);
    }

    /**
     * Unlocks the account represented by this instance.
     */
    public void unlock() {
        if (types == null) {
            return;
        }
        types.remove(Vocabulary.s_c_locked_user);
    }

    /**
     * Enables the account represented by this instance.
     * <p>
     * Does nothing if the account is already enabled.
     */
    public void enable() {
        if (types == null) {
            return;
        }
        types.remove(Vocabulary.s_c_disabled_user);
    }

    /**
     * Checks whether the account represented by this instance is enabled.
     */
    public boolean isEnabled() {
        return types == null || !types.contains(Vocabulary.s_c_disabled_user);
    }

    /**
     * Disables the account represented by this instance.
     * <p>
     * Disabled account cannot be logged into and cannot be used to view/modify data.
     */
    public void disable() {
        addType(Vocabulary.s_c_disabled_user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{" +
                firstName +
                " " + lastName +
                ", username='" + username + '\'' +
                '}';
    }
}
