package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.GenericDao;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseRepositoryServiceImpl extends BaseRepositoryService<User> {

    private final UserDao userDao;

    @Autowired
    public BaseRepositoryServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    protected GenericDao<User> getPrimaryDao() {
        return userDao;
    }
}
