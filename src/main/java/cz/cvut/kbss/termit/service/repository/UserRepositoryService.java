package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.util.ValidationResult;
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
     * Finds a user with the specified username.
     *
     * @param username Username to search by
     * @return User with matching username
     */
    public Optional<User> findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    protected void prePersist(User instance) {
        final ValidationResult<User> validationResult = ValidationResult.of(validator.validate(instance));
        if (!validationResult.isValid()) {
            throw new ValidationException(validationResult);
        }
        instance.setPassword(passwordEncoder.encode(instance.getPassword()));
    }
}
