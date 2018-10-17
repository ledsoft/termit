package cz.cvut.kbss.termit.environment.config;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.service.Services;
import cz.cvut.kbss.termit.service.document.html.DummySelectorGenerator;
import cz.cvut.kbss.termit.service.document.html.HtmlSelectorGenerators;
import cz.cvut.kbss.termit.util.Constants;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@Configuration
@ComponentScan(basePackageClasses = {Services.class})
public class TestServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate client = new RestTemplate();
        final MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(Environment.getObjectMapper());
        final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName(
                Constants.UTF_8_ENCODING));
        client.setMessageConverters(
                Arrays.asList(jacksonConverter, stringConverter, new ResourceHttpMessageConverter()));
        return client;
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    @Primary
    public HtmlSelectorGenerators htmlSelectorGenerators() {
        return new HtmlSelectorGenerators() {
            @Override
            public Set<TermSelector> generateSelectors(Element... elements) {
                return Collections.singleton(new DummySelectorGenerator().generateSelector(elements));
            }
        };
    }

    @Bean
    public ClassPathResource languageSpecification() {
        return new ClassPathResource("language.ttl");
    }
}
