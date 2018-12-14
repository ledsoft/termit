package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.environment.config.TestRestSecurityConfig;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.handler.RestExceptionHandler;
import cz.cvut.kbss.termit.security.JwtUtils;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import java.util.Collections;
import java.util.List;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static cz.cvut.kbss.termit.service.IdentifierResolver.extractIdentifierFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This tests only the security aspect of {@link UserController}. Functionality is tested in {@link
 * UserControllerTest}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class,
                                 TestRestSecurityConfig.class,
                                 UserControllerSecurityTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
class UserControllerSecurityTest extends BaseControllerTestRunner {

    private static final String BASE_URL = "/users";

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepositoryService userService;

    @Autowired
    private SecurityUtils securityUtilsMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setupObjectMappers();
        // WebApplicationContext is required for proper security. Otherwise, standaloneSetup could be used
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity(springSecurityFilterChain))
                                      .build();
    }

    /**
     * Inner class is necessary to provide the controller as a bean, so that the WebApplicationContext can map it.
     */
    @EnableWebMvc
    @Configuration
    public static class Config implements WebMvcConfigurer {
        @Mock
        private UserRepositoryService userService;

        @Mock
        private SecurityUtils securityUtilsMock;

        @InjectMocks
        private UserController controller;

        Config() {
            MockitoAnnotations.initMocks(this);
        }

        @Bean
        public UserRepositoryService userService() {
            return userService;
        }

        @Bean
        public UserController personController() {
            return controller;
        }

        @Bean
        public SecurityUtils securityUtils() {
            return securityUtilsMock;
        }

        @Bean
        public RestExceptionHandler restExceptionHandler() {
            return new RestExceptionHandler();
        }

        @Bean
        public JwtUtils jwtUtils(cz.cvut.kbss.termit.util.Configuration config) {
            return new JwtUtils(config);
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(Environment.createJsonLdMessageConverter());
            converters.add(Environment.createDefaultMessageConverter());
            converters.add(Environment.createStringEncodingMessageConverter());
        }
    }

    @Test
    void findAllThrowsForbiddenForUnauthorizedUser() throws Exception {
        Environment.setCurrentUser(generateAccount());
        when(userService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
        verify(userService, never()).findAll();
    }

    @Test
    void getCurrentReturnsCurrentlyLoggedInUser() throws Exception {
        final UserAccount user = generateAccount();
        Environment.setCurrentUser(user);
        final MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/current").accept(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isOk()).andReturn();
        final UserAccount result = readValue(mvcResult, UserAccount.class);
        assertEquals(user, result);
    }

    @Test
    void unlockThrowsForbiddenForNonAdmin() throws Exception {
        // This one is not an admin
        Environment.setCurrentUser(generateAccount());
        final UserAccount toUnlock = generateAccount();

        mockMvc.perform(
                delete(BASE_URL + "/" + extractIdentifierFragment(toUnlock.getUri()) + "/lock")
                        .content(toUnlock.getPassword()))
               .andExpect(status().isForbidden());
        verify(userService, never()).unlock(any(), any());
    }

    @Test
    void enableThrowsForbiddenForNonAdmin() throws Exception {
        // This one is not an admin
        Environment.setCurrentUser(generateAccount());
        final UserAccount toEnable = generateAccount();

        mockMvc.perform(post(BASE_URL + "/" + extractIdentifierFragment(toEnable.getUri()) + "/status"))
               .andExpect(status().isForbidden());
        verify(userService, never()).enable(any());
    }

    @Test
    void disableThrowsForbiddenForNonAdmin() throws Exception {
        // This one is not an admin
        Environment.setCurrentUser(generateAccount());
        final UserAccount toDisable = generateAccount();

        mockMvc.perform(delete(BASE_URL + "/" + extractIdentifierFragment(toDisable.getUri()) + "/status"))
               .andExpect(status().isForbidden());
        verify(userService, never()).disable(any());
    }
}