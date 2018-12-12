package cz.cvut.kbss.termit.environment.config;

import com.github.ledsoft.jopa.spring.transaction.DelegatingEntityManager;
import com.github.ledsoft.jopa.spring.transaction.JopaTransactionManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.termit.environment.TestPersistenceFactory;
import cz.cvut.kbss.termit.model.util.MetamodelUtils;
import cz.cvut.kbss.termit.persistence.Persistence;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = {Persistence.class})
@Import({TestPersistenceFactory.class})
@EnableTransactionManagement
public class TestPersistenceConfig {

    @Bean
    public DelegatingEntityManager entityManager() {
        return new DelegatingEntityManager();
    }

    @Bean(name = "txManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf, DelegatingEntityManager emProxy) {
        return new JopaTransactionManager(emf, emProxy);
    }

    @Bean
    public MetamodelUtils metamodelUtils(EntityManagerFactory emf) {
        return new MetamodelUtils(emf);
    }
}
