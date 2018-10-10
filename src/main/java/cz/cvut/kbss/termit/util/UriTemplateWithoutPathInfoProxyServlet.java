package cz.cvut.kbss.termit.util;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;

/**
 * A servlet wrapper which throws away path info from the servlet request.
 */
public class UriTemplateWithoutPathInfoProxyServlet extends URITemplateProxyServlet {
    @Override protected void service(HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse)
        throws ServletException, IOException {
        super.service(new HttpServletRequestWrapper(servletRequest) {
            @Override public String getPathInfo() {
                return "";
            }
        }, servletResponse);
    }
}
