package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLDataProperty;
import cz.cvut.kbss.jopa.model.annotations.ParticipationConstraints;
import cz.cvut.kbss.termit.util.Vocabulary;

import javax.validation.constraints.NotBlank;
import java.util.HashSet;

@OWLClass(iri = Vocabulary.s_c_uzivatel_termitu)
public class UserAccount extends AbstractUser {

    @NotBlank
    @ParticipationConstraints(nonEmpty = true)
    @OWLDataProperty(iri = cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_heslo)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return types != null && types.contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_uzamceny_uzivatel_termitu);
    }

    /**
     * Locks the account represented by this instance.
     */
    public void lock() {
        addType(cz.cvut.kbss.termit.util.Vocabulary.s_c_uzamceny_uzivatel_termitu);
    }

    /**
     * Unlocks the account represented by this instance.
     */
    public void unlock() {
        if (types == null) {
            return;
        }
        types.remove(cz.cvut.kbss.termit.util.Vocabulary.s_c_uzamceny_uzivatel_termitu);
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
        types.remove(cz.cvut.kbss.termit.util.Vocabulary.s_c_zablokovany_uzivatel_termitu);
    }

    /**
     * Checks whether the account represented by this instance is enabled.
     */
    public boolean isEnabled() {
        return types == null || !types.contains(cz.cvut.kbss.termit.util.Vocabulary.s_c_zablokovany_uzivatel_termitu);
    }

    /**
     * Disables the account represented by this instance.
     * <p>
     * Disabled account cannot be logged into and cannot be used to view/modify data.
     */
    public void disable() {
        addType(Vocabulary.s_c_zablokovany_uzivatel_termitu);
    }

    /**
     * Transforms this security-related {@code UserAccount} instance to a domain-specific {@code User} instance.
     *
     * @return new user instance based on this account
     */
    public User toUser() {
        final User user = new User();
        user.setUri(uri);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        if (types != null) {
            user.setTypes(new HashSet<>(types));
        }
        return user;
    }
}
