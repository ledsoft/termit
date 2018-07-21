package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.rest.servlet.DiagnosticsContextFilter;
import cz.cvut.kbss.termit.security.SecurityConstants;
import cz.cvut.kbss.termit.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.*;
import java.util.Collections;
import java.util.EnumSet;

/**
 * Entry point of the application invoked by the application server on deploy.
 */
public class DispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherServletInitializer.class);

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{AppConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return null;
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/rest/*"};
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        printStartupMessage();

        initSecurityFilter(servletContext);
        initMdcFilter(servletContext);
        servletContext.addListener(new RequestContextListener());
        servletContext.getSessionCookieConfig().setName(SecurityConstants.SESSION_COOKIE_NAME);
    }

    private void printStartupMessage() {
        final String msg = "* TermIt " + Constants.VERSION + " *";
        LOG.info(String.join("", Collections.nCopies(msg.length(), "*")));
        LOG.info(msg);
        LOG.info(String.join("", Collections.nCopies(msg.length(), "*")));
    }

    /**
     * Initializes Spring Security servlet filter
     */
    private void initSecurityFilter(ServletContext servletContext) {
        FilterRegistration.Dynamic securityFilter = servletContext.addFilter("springSecurityFilterChain",
                DelegatingFilterProxy.class);
        final EnumSet<DispatcherType> es = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);
        securityFilter.addMappingForUrlPatterns(es, true, "/*");
    }

    /**
     * Initializes diagnostics context filter for logging session info
     */
    private void initMdcFilter(ServletContext servletContext) {
        FilterRegistration.Dynamic mdcFilter = servletContext
                .addFilter("diagnosticsContextFilter", new DiagnosticsContextFilter());
        final EnumSet<DispatcherType> es = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);
        mdcFilter.addMappingForUrlPatterns(es, true, "/*");
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(getMultipartConfigElement());
    }

    /**
     * Configures multipart processing (for file upload).
     */
    private MultipartConfigElement getMultipartConfigElement() {
        return new MultipartConfigElement(Constants.UPLOADED_FILE_LOCATION, Constants.MAX_UPLOADED_FILE_SIZE,
                Constants.MAX_UPLOAD_REQUEST_SIZE, Constants.UPLOADED_FILE_SIZE_THRESHOLD);
    }
}
