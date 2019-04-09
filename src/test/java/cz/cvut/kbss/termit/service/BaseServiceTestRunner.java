package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.TransactionalTestRunner;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.environment.config.TestPersistenceConfig;
import cz.cvut.kbss.termit.environment.config.TestServiceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.URI;

@ExtendWith(SpringExtension.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
@ContextConfiguration(classes = {TestConfig.class, TestPersistenceConfig.class, TestServiceConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BaseServiceTestRunner extends TransactionalTestRunner {

    private static final String EXISTENCE_CHECK_QUERY = "ASK { ?x a ?type . }";

    protected void verifyInstancesDoNotExist(String type, EntityManager em) {
        Assertions.assertFalse(
                em.createNativeQuery(EXISTENCE_CHECK_QUERY, Boolean.class).setParameter("type", URI.create(type))
                  .getSingleResult());
    }
}
