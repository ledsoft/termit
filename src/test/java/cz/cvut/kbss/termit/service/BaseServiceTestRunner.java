package cz.cvut.kbss.termit.service;

import cz.cvut.kbss.termit.environment.Transaction;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.environment.config.TestPersistenceConfig;
import cz.cvut.kbss.termit.environment.config.TestServiceConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class, TestPersistenceConfig.class, TestServiceConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BaseServiceTestRunner {

    @Autowired
    protected PlatformTransactionManager txManager;

    protected void transactional(Runnable procedure) {
        Transaction.execute(txManager, procedure);
    }
}
