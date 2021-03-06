/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.environment.config.TestConfig;
import cz.cvut.kbss.termit.environment.config.TestRestSecurityConfig;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.handler.RestExceptionHandler;
import cz.cvut.kbss.termit.security.JwtUtils;
import cz.cvut.kbss.termit.service.business.UserService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.Vocabulary;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class,
        TestRestSecurityConfig.class,
        AdminBasedRegistrationControllerTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebAppConfiguration
class AdminBasedRegistrationControllerTest extends BaseControllerTestRunner {

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

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
        private UserService userService;

        @Mock
        private SecurityUtils securityUtilsMock;

        @InjectMocks
        private AdminBasedRegistrationController controller;

        Config() {
            MockitoAnnotations.initMocks(this);
        }

        @Bean
        public UserService userService() {
            return userService;
        }

        @Bean
        public AdminBasedRegistrationController registrationController() {
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
    void createUserPersistsUserWhenCalledByAdmin() throws Exception {
        final UserAccount admin = Generator.generateUserAccount();
        admin.addType(Vocabulary.s_c_administrator_termitu);
        Environment.setCurrentUser(admin);
        final UserAccount user = Generator.generateUserAccount();
        mockMvc.perform(post("/users").content(toJson(user)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isCreated());
        verify(userService).persist(user);
    }

    @Test
    void createUserThrowsForbiddenForNonAdminUser() throws Exception {
        final UserAccount admin = Generator.generateUserAccount();
        Environment.setCurrentUser(admin);
        final UserAccount user = Generator.generateUserAccount();
        mockMvc.perform(post("/users").content(toJson(user)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isForbidden());
        verify(userService, never()).persist(any());
    }
}
