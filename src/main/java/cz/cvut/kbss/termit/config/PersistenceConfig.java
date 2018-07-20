package cz.cvut.kbss.termit.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "cz.cvut.kbss.termit.persistence")
public class PersistenceConfig {
}
