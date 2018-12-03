package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import cz.cvut.kbss.termit.security.model.TermItUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TermItUserDetailsService implements UserDetailsService {

    private final UserAccountDao userAccountDao;

    @Autowired
    public TermItUserDetailsService(UserAccountDao userAccountDao) {
        this.userAccountDao = userAccountDao;
    }

    @Override
    public TermItUserDetails loadUserByUsername(String username) {
        return new TermItUserDetails(userAccountDao.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("User with username " + username + " not found.")));
    }
}
