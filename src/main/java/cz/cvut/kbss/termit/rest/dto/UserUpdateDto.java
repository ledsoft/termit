package cz.cvut.kbss.termit.rest.dto;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.util.Objects;

/**
 * DTO used for user updating so that original password can be validated.
 */
@OWLClass(iri = Vocabulary.s_c_uzivatel_termitu)
public class UserUpdateDto extends UserAccount {

    @OWLDataProperty(iri = Vocabulary.ONTOLOGY_IRI_slovnik + "/original-password")
    private String originalPassword;

    public String getOriginalPassword() {
        return originalPassword;
    }

    public void setOriginalPassword(String originalPassword) {
        this.originalPassword = originalPassword;
    }

    /**
     * Transforms this DTO to the regular entity.
     * <p>
     * This is necessary for correct persistence processing, as this class is not a known entity class.
     *
     * @return {@link User} instance
     */
    public UserAccount asUserAccount() {
        final UserAccount user = new UserAccount();
        user.setUri(getUri());
        user.setFirstName(getFirstName());
        user.setLastName(getLastName());
        user.setUsername(getUsername());
        user.setPassword(getPassword());
        user.setTypes(getTypes());
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserUpdateDto)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        UserUpdateDto that = (UserUpdateDto) o;
        return Objects.equals(originalPassword, that.originalPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), originalPassword);
    }
}
