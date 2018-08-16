package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserRepositoryService userService;

    private final SecurityUtils securityUtils;

    @Autowired
    public UserController(UserRepositoryService userService, SecurityUtils securityUtils,
                          IdentifierResolver idResolver, Configuration config) {
        super(idResolver, config);
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<User> getAll() {
        return userService.findAll();
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public ResponseEntity<Void> createUser(@RequestBody User user) {
        userService.persist(user);
        LOG.info("User {} successfully registered.", user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value = "/current", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public User getCurrent(Principal principal) {
        final AuthenticationToken auth = (AuthenticationToken) principal;
        return auth.getDetails().getUser();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/current", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public void updateCurrent(@RequestBody UserUpdateDto update) {
        if (update.getPassword() != null) {
            securityUtils.verifyCurrentUserPassword(update.getOriginalPassword());
        }
        final User user = update.toUser();
        userService.update(user);
        LOG.debug("User {} successfully updated.", user);
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @RequestMapping(value = "/{fragment}/lock", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlock(@PathVariable(name = "fragment") String identifierFragment, @RequestBody String newPassword) {
        final URI id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_USER, identifierFragment);
        final Optional<User> toUnlock = userService.find(id);
        final User user = toUnlock.orElseThrow(() -> NotFoundException.create("User", id));
        userService.unlock(user, newPassword);
        LOG.debug("User {} successfully unlocked.", user);
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @RequestMapping(value = "/{fragment}/status", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable(name = "fragment") String identifierFragment) {
        final URI id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_USER, identifierFragment);
        final Optional<User> toEnable = userService.find(id);
        final User user = toEnable.orElseThrow(() -> NotFoundException.create("User", id));
        userService.enable(user);
        LOG.debug("User {} successfully enabled.", user);
    }

    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    @RequestMapping(value = "/{fragment}/status", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable(name = "fragment") String identifierFragment) {
        final URI id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_USER, identifierFragment);
        final Optional<User> toDisable = userService.find(id);
        final User user = toDisable.orElseThrow(() -> NotFoundException.create("User", id));
        userService.disable(user);
        LOG.debug("User {} successfully disabled.", user);
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/username")
    public Boolean exists(@RequestParam(name = "username") String username) {
        return userService.exists(username);
    }
}
