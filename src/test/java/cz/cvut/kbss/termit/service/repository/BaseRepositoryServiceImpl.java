package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Validator;

public class BaseRepositoryServiceImpl extends BaseRepositoryService<UserAccount> {

    private final UserAccountDao userAccountDao;

    @Autowired
    public BaseRepositoryServiceImpl(UserAccountDao userAccountDao, Validator validator) {
        super(validator);
        this.userAccountDao = userAccountDao;
    }

    @Override
    protected GenericDao<UserAccount> getPrimaryDao() {
        return userAccountDao;
    }
}
