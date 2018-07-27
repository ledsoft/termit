package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.security.SecurityConstants;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Base for application REST controllers.
 *
 * Will be used to define general security for the public API.
 */
@PreAuthorize("hasRole('" + SecurityConstants.ROLE_USER + "')")
public class BaseController {
}
