package cz.cvut.kbss.termit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.service.Services;
import cz.cvut.kbss.termit.service.SystemInitializer;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;

@Configuration
@ComponentScan(basePackageClasses = {Services.class})
public class ServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate(@Qualifier("objectMapper") ObjectMapper objectMapper) {
        final RestTemplate restTemplate = new RestTemplate();

        // Using LaxRedirectStrategy to allow redirects of POST, PUT and DELETE requests
        // Introduced here because text analysis invocations (POST) were redirected and the resulting documents were
        // malformed (contained the redirect page instead of the result).
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        final HttpClient httpClient = HttpClientBuilder.create()
                                                       .setRedirectStrategy(new LaxRedirectStrategy())
                                                       .build();
        factory.setHttpClient(httpClient);
        restTemplate.setRequestFactory(factory);

        final MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);
        final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName(
                Constants.UTF_8_ENCODING));
        restTemplate.setMessageConverters(
                Arrays.asList(jacksonConverter, stringConverter, new ResourceHttpMessageConverter()));
        return restTemplate;
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

    @Bean
    public ClassPathResource languageSpecification() {
        return new ClassPathResource("language.ttl");
    }
}
