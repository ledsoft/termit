package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.config.Configuration;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserDao extends BaseDao<User> {

    private final Configuration config;

    @Autowired
    public UserDao(EntityManager em, Configuration config) {
        super(User.class, em);
        this.config = config;
    }

    /**
     * Finds a user with the specified username.
     *
     * @param username Username to search by
     * @return User with matching username
     */
    public Optional<User> findByUsername(String username) {
        Objects.requireNonNull(username);
        try {
            return Optional
                    .of(em.createNativeQuery("SELECT ?x WHERE { ?x a ?type ; ?hasUsername ?username . }", User.class)
                          .setParameter("type", typeUri)
                          .setParameter("hasUsername", URI.create(Vocabulary.s_p_username))
                          .setParameter("username", username, config.get(ConfigParam.LANGUAGE)).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
