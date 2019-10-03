package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.UserService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService, IdentifierResolver idResolver, Configuration config) {
        super(idResolver, config);
        this.userService = userService;
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<UserAccount> getAll() {
        return userService.findAll();
    }

    @GetMapping(value = "/current", produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public UserAccount getCurrent() {
        return userService.getCurrent();
    }

    @PutMapping(value = "/current", consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public UserAccount updateCurrent(@RequestBody UserUpdateDto update) {
        userService.updateCurrent(update);
        LOG.debug("User {} successfully updated.", update);
        return getCurrent();
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @DeleteMapping(value = "/{fragment}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlock(@PathVariable(name = "fragment") String identifierFragment, @RequestBody String newPassword) {
        final UserAccount user = getUserAccountForUpdate(identifierFragment);
        userService.unlock(user, newPassword);
        LOG.debug("User {} successfully unlocked.", user);
    }

    private UserAccount getUserAccountForUpdate(String identifierFragment) {
        final URI id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_USER, identifierFragment);
        return userService.findRequired(id);
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @PostMapping(value = "/{fragment}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable(name = "fragment") String identifierFragment) {
        final UserAccount user = getUserAccountForUpdate(identifierFragment);
        userService.enable(user);
        LOG.debug("User {} successfully enabled.", user);
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @DeleteMapping(value = "/{fragment}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable(name = "fragment") String identifierFragment) {
        final UserAccount user = getUserAccountForUpdate(identifierFragment);
        userService.disable(user);
        LOG.debug("User {} successfully disabled.", user);
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/username")
    public Boolean exists(@RequestParam(name = "username") String username) {
        return userService.exists(username);
    }
}
