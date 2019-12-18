package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.aspect.Aspects;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@EnableMBeanExport
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = Aspects.class)
@Import({PersistenceConfig.class, ServiceConfig.class, WebAppConfig.class})
@PropertySource("classpath:config.properties")
public class AppConfig {

    @Bean
    public cz.cvut.kbss.termit.util.Configuration configuration(Environment environment) {
        return new cz.cvut.kbss.termit.util.Configuration(environment);
    }
}
