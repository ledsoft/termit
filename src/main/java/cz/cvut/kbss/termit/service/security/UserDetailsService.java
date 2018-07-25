package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.persistence.dao.UserDao;
import cz.cvut.kbss.termit.security.model.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserDao userDao;

    @Autowired
    public UserDetailsService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return new UserDetails(userDao.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("User with username " + username + " not found.")));
    }
}
