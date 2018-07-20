package cz.cvut.kbss.termit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;

@Configuration
@EnableMBeanExport
@Import({PersistenceConfig.class})
public class AppConfig {
}
