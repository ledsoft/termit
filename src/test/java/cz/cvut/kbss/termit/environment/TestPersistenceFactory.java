package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.jopa.Persistence;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.ontodriver.sesame.config.SesameOntoDriverProperties;
import cz.cvut.kbss.termit.persistence.MainPersistenceFactory;
import cz.cvut.kbss.termit.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

import static cz.cvut.kbss.jopa.model.JOPAPersistenceProperties.*;
import static cz.cvut.kbss.termit.util.ConfigParam.*;

@Configuration
public class TestPersistenceFactory {

    private final cz.cvut.kbss.termit.config.Configuration config;

    private EntityManagerFactory emf;

    @Autowired
    public TestPersistenceFactory(cz.cvut.kbss.termit.config.Configuration config) {
        this.config = config;
    }

    @Bean
    @Primary
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    @PostConstruct
    private void init() {
        final Map<String, String> properties = MainPersistenceFactory.defaultParams();
        properties.put(ONTOLOGY_PHYSICAL_URI_KEY, config.get(REPOSITORY_URL));
        properties.put(SesameOntoDriverProperties.SESAME_USE_VOLATILE_STORAGE, Boolean.TRUE.toString());
        properties.put(DATA_SOURCE_CLASS, config.get(DRIVER));
        properties.put(LANG, config.get(LANGUAGE, Constants.DEFAULT_LANGUAGE));
        this.emf = Persistence.createEntityManagerFactory("termitTestPU", properties);
    }

    @PreDestroy
    private void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
}
