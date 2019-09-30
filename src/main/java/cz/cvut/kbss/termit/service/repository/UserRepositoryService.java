package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class UserRepositoryService extends BaseRepositoryService<UserAccount> {

    private final UserAccountDao userAccountDao;

    private final IdentifierResolver idResolver;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserRepositoryService(UserAccountDao userAccountDao, IdentifierResolver idResolver,
                                 PasswordEncoder passwordEncoder, Validator validator) {
        super(validator);
        this.userAccountDao = userAccountDao;
        this.idResolver = idResolver;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected GenericDao<UserAccount> getPrimaryDao() {
        return userAccountDao;
    }

    /**
     * Checks whether a user with the specified username exists.
     *
     * @param username Username to search by
     * @return {@code true} if a user with the specifier username exists
     */
    public boolean exists(String username) {
        return userAccountDao.exists(username);
    }

    @Override
    protected UserAccount postLoad(UserAccount instance) {
        instance.erasePassword();
        return instance;
    }

    @Override
    protected void prePersist(UserAccount instance) {
        validate(instance);
        if (instance.getUri() == null) {
            instance.setUri(idResolver
                    .generateIdentifier(ConfigParam.NAMESPACE_USER, instance.getFirstName(), instance.getLastName()));
        }
        instance.setPassword(passwordEncoder.encode(instance.getPassword()));
        instance.addType(Vocabulary.s_c_omezeny_uzivatel_termitu);
        instance.removeType(Vocabulary.s_c_administrator_termitu);
    }

    @Override
    protected void preUpdate(UserAccount instance) {
        final UserAccount original = userAccountDao.find(instance.getUri()).orElseThrow(
                () -> new NotFoundException("User " + instance + " does not exist."));
        if (instance.getPassword() != null) {
            instance.setPassword(passwordEncoder.encode(instance.getPassword()));
        } else {
            instance.setPassword(original.getPassword());
        }
        validate(instance);
    }
}
