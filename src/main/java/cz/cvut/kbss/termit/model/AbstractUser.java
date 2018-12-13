package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.termit.model.util.HasIdentifier;
import cz.cvut.kbss.termit.model.util.HasTypes;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.net.URI;
import java.util.Objects;
import java.util.Set;

@MappedSuperclass
abstract class AbstractUser implements HasIdentifier, HasTypes, Serializable {

    @Id
    URI uri;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_krestni_jmeno)
    String firstName;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_prijmeni)
    String lastName;

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = Vocabulary.s_p_ma_uzivatelske_jmeno)
    String username;

    @Types
    Set<String> types;

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
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

    @Override
    public Set<String> getTypes() {
        return types;
    }

    @Override
    public void setTypes(Set<String> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractUser)) {
            return false;
        }
        AbstractUser that = (AbstractUser) o;
        return Objects.equals(uri, that.uri) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, username);
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
