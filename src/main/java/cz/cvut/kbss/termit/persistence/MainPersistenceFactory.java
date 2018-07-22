package cz.cvut.kbss.termit.persistence;

import cz.cvut.kbss.jopa.Persistence;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.JOPAPersistenceProvider;
import cz.cvut.kbss.ontodriver.config.OntoDriverProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

import static cz.cvut.kbss.jopa.model.JOPAPersistenceProperties.*;
import static cz.cvut.kbss.jopa.model.PersistenceProperties.JPA_PERSISTENCE_PROVIDER;
import static cz.cvut.kbss.termit.util.ConfigParam.*;

/**
 * Sets up persistence and provides {@link EntityManagerFactory} as Spring bean.
 */
@Configuration
@PropertySource("classpath:config.properties")
public class MainPersistenceFactory {

    private final Environment environment;

    private EntityManagerFactory emf;

    @Autowired
    public MainPersistenceFactory(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    @PostConstruct
    private void init() {
        // Allow Apache HTTP client used by RDF4J to use a larger connection pool
        // Temporary, should be configurable via JOPA
        System.setProperty("http.maxConnections", "20");
        final Map<String, String> properties = defaultParams();
        properties.put(ONTOLOGY_PHYSICAL_URI_KEY, environment.getProperty(REPOSITORY_URL.toString()));
        properties.put(DATA_SOURCE_CLASS, environment.getProperty(DRIVER.toString()));
        properties.put(LANG, environment.getProperty(LANGUAGE.toString()));
        if (environment.containsProperty(REPO_USERNAME.toString())) {
            properties
                    .put(OntoDriverProperties.DATA_SOURCE_USERNAME, environment.getProperty(REPO_USERNAME.toString()));
            properties
                    .put(OntoDriverProperties.DATA_SOURCE_PASSWORD, environment.getProperty(REPO_PASSWORD.toString()));
        }
        this.emf = Persistence.createEntityManagerFactory("termitPU", properties);
    }

    @PreDestroy
    private void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Default persistence unit configuration parameters.
     * <p>
     * These include: package scan for entities, provider specification
     *
     * @return Map with defaults
     */
    public static Map<String, String> defaultParams() {
        final Map<String, String> map = new HashMap<>();
        map.put(SCAN_PACKAGE, "cz.cvut.kbss.termit.model");
        map.put(JPA_PERSISTENCE_PROVIDER, JOPAPersistenceProvider.class.getName());
        return map;
    }
}
