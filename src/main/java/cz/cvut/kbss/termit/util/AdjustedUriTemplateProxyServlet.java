package cz.cvut.kbss.termit.util;

import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.springframework.http.HttpHeaders;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * A servlet wrapper which 1. throws away path info from the servlet request. 2. contains unique header keys (to avoid
 * Access-Control-Header-Origin duplication). 3. Uses Basic authentication in case repository username/password are
 * configured.
 */
public class AdjustedUriTemplateProxyServlet extends URITemplateProxyServlet {

    @Override
    protected void service(HttpServletRequest servletRequest,
                           HttpServletResponse servletResponse)
            throws ServletException, IOException {
        final String username = getConfigParam(ConfigParam.REPO_USERNAME.toString());
        final String password = getConfigParam(ConfigParam.REPO_USERNAME.toString());
        super.service(new AuthenticatingServletRequestWrapper(servletRequest, username, password),
                new HttpServletResponseWrapper(servletResponse) {
                    @Override
                    public void addHeader(String name, String value) {
                        if (containsHeader(name)) {
                            return;
                        }
                        super.addHeader(name, value);
                    }
                });
    }

    static class AuthenticatingServletRequestWrapper extends HttpServletRequestWrapper {

        private final String username;
        private final String password;

        AuthenticatingServletRequestWrapper(HttpServletRequest request, String username, String password) {
            super(request);
            this.username = username;
            this.password = password;
        }

        @Override
        public String getPathInfo() {
            return "";
        }

        @Override
        public String getHeader(String name) {
            if (name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION) && !username.isEmpty()) {
                return createBasicAuthentication();
            }
            return super.getHeader(name);
        }

        private String createBasicAuthentication() {
            String encoding = Base64.getEncoder()
                                    .encodeToString((username.concat(":").concat(password).getBytes()));
            return "Basic " + encoding;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION) && !username.isEmpty()) {
                return Collections.enumeration(Collections.singletonList(createBasicAuthentication()));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            if (!username.isEmpty()) {
                List<String> temp = Collections.list(super.getHeaderNames());
                temp.add(HttpHeaders.AUTHORIZATION);
                return Collections.enumeration(temp);
            }
            return super.getHeaderNames();
        }
    }
}
