package cz.cvut.kbss.termit.service.jmx;

import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName = "bean:name=TermItAdminBean", description = "TermIt administration JMX bean.")
public class AppAdminBean {

    private static final Logger LOG = LoggerFactory.getLogger(AppAdminBean.class);

    private final EntityManagerFactory emf;

    @Autowired
    public AppAdminBean(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @ManagedOperation(description = "Invalidates the application caches.")
    public void invalidateCaches() {
        LOG.info("Invalidating application caches...");
        emf.getCache().evictAll();
    }
}
