package cz.cvut.kbss.termit.config;

import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableMBeanExport
@Import({PersistenceConfig.class})
@PropertySource("classpath:config.properties")
public class AppConfig {

    @Bean
    public cz.cvut.kbss.termit.util.Configuration configuration(Environment environment) {
        return new cz.cvut.kbss.termit.util.Configuration(environment);
    }
}
