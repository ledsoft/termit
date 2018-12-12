package cz.cvut.kbss.termit.config;

import com.github.ledsoft.jopa.spring.transaction.DelegatingEntityManager;
import com.github.ledsoft.jopa.spring.transaction.JopaTransactionManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.termit.model.util.MetamodelUtils;
import cz.cvut.kbss.termit.persistence.MainPersistenceFactory;
import cz.cvut.kbss.termit.persistence.Persistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import(MainPersistenceFactory.class)
@ComponentScan(basePackageClasses = Persistence.class)
public class PersistenceConfig {

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
