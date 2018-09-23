package cz.cvut.kbss.termit.rest.servlet;

import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.security.model.AuthenticationToken;
import cz.cvut.kbss.termit.security.model.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Collections;

import static cz.cvut.kbss.termit.model.UserAccountTest.generateAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class DiagnosticsContextFilterTest {

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Mock
    private FilterChain chainMock;

    private DiagnosticsContextFilter filter = new DiagnosticsContextFilter();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void setsDiagnosticsContextWhenProcessingChain() throws Exception {
        final UserAccount user = generateAccount();
        final Principal token = new AuthenticationToken(Collections.emptyList(), new UserDetails(user));
        when(requestMock.getUserPrincipal()).thenReturn(token);
        doAnswer((answer) -> {
            assertEquals(user.getUsername(), MDC.get(DiagnosticsContextFilter.MDC_KEY));
            return null;
        }).when(chainMock).doFilter(requestMock, responseMock);

        filter.doFilter(requestMock, responseMock, chainMock);
        verify(chainMock).doFilter(requestMock, responseMock);
    }

    @Test
    void doesNotSetDiagnosticsContextForAnonymousPrincipal() throws Exception {
        when(requestMock.getUserPrincipal()).thenReturn(null);
        doAnswer((answer) -> {
            assertNull(MDC.get(DiagnosticsContextFilter.MDC_KEY));
            return null;
        }).when(chainMock).doFilter(requestMock, responseMock);

        filter.doFilter(requestMock, responseMock, chainMock);
        verify(chainMock).doFilter(requestMock, responseMock);
    }
}