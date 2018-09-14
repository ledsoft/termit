package cz.cvut.kbss.termit.config;

import cz.cvut.kbss.termit.service.Services;
import cz.cvut.kbss.termit.service.SystemInitializer;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

@Configuration
@ComponentScan(basePackageClasses = {Services.class})
public class ServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Provides JSR 380 validator for bean validation.
     */
    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public SystemInitializer systemInitializer(cz.cvut.kbss.termit.util.Configuration config,
                                               UserRepositoryService userService,
                                               PlatformTransactionManager txManager) {
        return new SystemInitializer(config, userService, txManager);
    }
}
