/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
