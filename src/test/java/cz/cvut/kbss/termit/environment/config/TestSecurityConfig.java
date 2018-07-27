package cz.cvut.kbss.termit.environment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.security.Security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.mock;

@Configuration
@ComponentScan(basePackageClasses = {Security.class})
public class TestSecurityConfig {

    @Bean
    @Primary
    public HttpSession getSession() {
        return new MockHttpSession();
    }

    @Bean
    public HttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Environment.getObjectMapper();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return mock(AuthenticationManager.class);
    }
}
