package cz.cvut.kbss.termit.util;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;

/**
 * A servlet wrapper which
 *  1. throws away path info from the servlet request.
 *  2. contains unique header keys (to avoid Access-Control-Header-Origin duplication)
 */
public class AdjustedUriTemplateProxyServlet extends URITemplateProxyServlet {
    @Override protected void service(HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse)
        throws ServletException, IOException {
        super.service(new HttpServletRequestWrapper(servletRequest) {
            @Override public String getPathInfo() {
                return "";
            }
        }, new HttpServletResponseWrapper(servletResponse){
            @Override public void addHeader(String name, String value) {
                if ( containsHeader(name)) {
                    return;
                }
                super.addHeader(name, value);
            }
        });
    }
}
