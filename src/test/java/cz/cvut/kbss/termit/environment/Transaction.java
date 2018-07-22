package cz.cvut.kbss.termit.environment;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Utility class for executing operations in transaction.
 * <p>
 * It is required for proper JOPA functionality in tests. The tests cannot function in one transaction like regular
 * JPA-based Spring tests, since JOPA is not able to add pending changes to query results.
 */
public class Transaction {

    /**
     * Executes the specified procedure in a transaction managed by the specified transaction manager.
     *
     * @param txManager Transaction manager
     * @param procedure Code to execute
     */
    public static void execute(PlatformTransactionManager txManager, Runnable procedure) {
        new TransactionTemplate(txManager).execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                procedure.run();
            }
        });
    }
}
