package cz.cvut.kbss.termit.environment.config;

import cz.cvut.kbss.termit.service.Services;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
@ComponentScan(basePackageClasses = {Services.class})
public class TestServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }
}
