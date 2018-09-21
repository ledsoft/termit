package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.exception.AuthorizationException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserRepositoryService extends BaseRepositoryService<UserAccount> {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepositoryService.class);

    private final UserAccountDao userAccountDao;

    private final IdentifierResolver idResolver;

    private final SecurityUtils securityUtils;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserRepositoryService(UserAccountDao userAccountDao, IdentifierResolver idResolver,
                                 SecurityUtils securityUtils,
                                 PasswordEncoder passwordEncoder, Validator validator) {
        super(validator);
        this.userAccountDao = userAccountDao;
        this.idResolver = idResolver;
        this.securityUtils = securityUtils;
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
        if (!securityUtils.getCurrentUser().getUri().equals(instance.getUri())) {
            throw new AuthorizationException(
                    "User " + securityUtils.getCurrentUser() + " attempted to update a different user's account.");
        }
        final Optional<UserAccount> orig = userAccountDao.find(instance.getUri());
        if (!orig.isPresent()) {
            throw new NotFoundException("User " + instance + " does not exist.");
        }
        if (instance.getPassword() != null) {
            instance.setPassword(passwordEncoder.encode(instance.getPassword()));
        } else {
            instance.setPassword(orig.get().getPassword());
        }
        validate(instance);
    }

    @Transactional
    public void unlock(UserAccount user, String newPassword) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(newPassword);
        user.unlock();
        user.setPassword(passwordEncoder.encode(newPassword));
        userAccountDao.update(user);
        LOG.trace("Unlocked user {}.", user);
    }

    @Transactional
    public void enable(UserAccount user) {
        Objects.requireNonNull(user);
        user.enable();
        userAccountDao.update(user);
        LOG.trace("Enabled user {}.", user);
    }

    @Transactional
    public void disable(UserAccount user) {
        Objects.requireNonNull(user);
        user.disable();
        userAccountDao.update(user);
        LOG.trace("Disabled user {}.", user);
    }

    @EventListener
    @Transactional
    public void onLoginAttemptsThresholdExceeded(LoginAttemptsThresholdExceeded event) {
        final UserAccount toDisable = event.getUser();
        toDisable.lock();
        userAccountDao.update(toDisable);
        LOG.trace("Locked user {}.", toDisable);
    }
}
