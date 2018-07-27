package cz.cvut.kbss.termit.environment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.security.Security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

@Configuration
@ComponentScan(basePackageClasses = {Security.class})
public class TestSecurityConfig {

    @Bean
    public HttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Environment.getObjectMapper();
    }
}
