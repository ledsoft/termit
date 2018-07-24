package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.termit.environment.Transaction;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.environment.config.TestPersistenceConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class, TestPersistenceConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseDaoTestRunner {

    @Autowired
    protected PlatformTransactionManager txManager;

    /**
     * Since JOPA does not understand SPARQL queries, any DAO method using a query will not be able to see uncommitted
     * transactional changes. So the whole test cannot run in a single transaction, as is common in regular Spring testing.
     * <p>
     * Instead, we need to perform methods which change the state of the storage in transactions, so that the changes are
     * really committed into the storage.
     *
     * @param procedure Code to execute
     */
    protected void transactional(Runnable procedure) {
        Transaction.execute(txManager, procedure);
    }
}
