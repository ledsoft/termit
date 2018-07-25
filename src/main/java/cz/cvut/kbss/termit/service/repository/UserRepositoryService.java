package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.util.IdentifierUtils;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.util.ValidationResult;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.Optional;

@Service
public class UserRepositoryService extends BaseRepositoryService<User> {

    private final UserDao userDao;

    private final PasswordEncoder passwordEncoder;

    private final Validator validator;

    @Autowired
    public UserRepositoryService(UserDao userDao, PasswordEncoder passwordEncoder, Validator validator) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Override
    protected GenericDao<User> getPrimaryDao() {
        return userDao;
    }

    /**
     * Checks whether a user with the specified username exists.
     *
     * @param username Username to search by
     * @return {@code true} if a user with the specifier username exists
     */
    public boolean exists(String username) {
        return userDao.exists(username);
    }

    @Override
    protected User postLoad(User instance) {
        instance.erasePassword();
        return instance;
    }

    @Override
    protected void prePersist(User instance) {
        validateInstance(instance);
        instance.setUri(IdentifierUtils
                .generateIdentifier(Vocabulary.ONTOLOGY_IRI_termit, instance.getFirstName(), instance.getLastName()));
        instance.setPassword(passwordEncoder.encode(instance.getPassword()));
    }

    private void validateInstance(User instance) {
        final ValidationResult<User> validationResult = ValidationResult.of(validator.validate(instance));
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }
    }

    @Override
    protected void preUpdate(User instance) {
        final Optional<User> orig = userDao.find(instance.getUri());
        if (!orig.isPresent()) {
            throw new NotFoundException("User " + instance + " does not exist.");
        }
        if (instance.getPassword() != null) {
            instance.setPassword(passwordEncoder.encode(instance.getPassword()));
        } else {
            instance.setPassword(orig.get().getPassword());
        }
        validateInstance(instance);
    }
}
