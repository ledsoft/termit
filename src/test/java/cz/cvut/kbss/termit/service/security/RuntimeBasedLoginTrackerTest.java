package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.event.LoginAttemptsThresholdExceeded;
import cz.cvut.kbss.termit.event.LoginFailureEvent;
import cz.cvut.kbss.termit.event.LoginSuccessEvent;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.security.SecurityConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Configuration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RuntimeBasedLoginTrackerTest.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RuntimeBasedLoginTrackerTest {

    @Bean
    public LoginTracker loginTracker() {
        return new RuntimeBasedLoginTracker();
    }

    @Bean
    public LoginListener loginListener() {
        return spy(new LoginListener());
    }

    @Autowired
    private LoginTracker loginTracker;

    @Autowired
    private LoginListener listener;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
    }

    @Test
    void emitsThresholdExceededEventWhenMaximumLoginCountIsExceeded() {
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            assertNull(listener.user);
            loginTracker.onLoginFailure(new LoginFailureEvent(user));
        }
        loginTracker.onLoginFailure(new LoginFailureEvent(user));
        assertNotNull(listener.user);
        assertEquals(user, listener.user);
    }

    @Test
    void doesNotReemitThresholdExceededWhenAdditionalLoginAttemptsAreMade() {
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS * 2; i++) {
            loginTracker.onLoginFailure(new LoginFailureEvent(user));
        }
        verify(listener, times(1)).onEvent(ArgumentMatchers.any());
    }

    @Test
    void successfulLoginResetsCounter() {
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS - 1; i++) {
            loginTracker.onLoginFailure(new LoginFailureEvent(user));
        }
        loginTracker.onLoginSuccess(new LoginSuccessEvent(user));
        for (int i = 0; i < SecurityConstants.MAX_LOGIN_ATTEMPTS; i++) {
            loginTracker.onLoginFailure(new LoginFailureEvent(user));
        }
        verify(listener, never()).onEvent(ArgumentMatchers.any());
    }

    public static class LoginListener {

        private User user;

        @EventListener
        public void onEvent(LoginAttemptsThresholdExceeded event) {
            this.user = event.getUser();
        }
    }
}