package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.environment.Environment.extractFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTestRunner {

    private static final String BASE_URL = "/users";

    @Mock
    private UserRepositoryService userService;

    @Mock
    private SecurityUtils securityUtilsMock;

    @Mock
    private IdentifierResolver idResolverMock;

    @InjectMocks
    private UserController sut;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.user = Generator.generateUserWithId();
        Environment.setCurrentUser(user);
    }

    @Test
    void getAllReturnsAllUsers() throws Exception {
        final List<User> users = IntStream.range(0, 5).mapToObj(i -> Generator.generateUserWithId())
                                          .collect(Collectors.toList());
        when(userService.findAll()).thenReturn(users);

        final MvcResult mvcResult = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isOk()).andReturn();
        final List<User> result = readValue(mvcResult, new TypeReference<List<User>>() {
        });
        assertEquals(users, result);
    }

    @Test
    void createUserPersistsUser() throws Exception {
        final User user = Generator.generateUser();
        mockMvc.perform(post(BASE_URL).content(toJson(user)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isCreated());
        verify(userService).persist(user);
    }

    @Test
    void updateCurrentVerifiesOriginalPasswordWhenNewOneIsSet() throws Exception {
        final UserUpdateDto dto = dtoForUpdate();

        mockMvc.perform(
                put(BASE_URL + "/current").content(toJson(dto)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isNoContent());
        verify(securityUtilsMock).verifyCurrentUserPassword(user.getPassword());
        verify(userService).update(user);
    }

    private UserUpdateDto dtoForUpdate() {
        final UserUpdateDto dto = new UserUpdateDto();
        dto.setUri(user.getUri());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPassword("newPassword");
        dto.setUsername(user.getUsername());
        dto.setOriginalPassword(user.getPassword());
        return dto;
    }

    @Test
    void updateCurrentReturnsConflictWithValidationMessageWhenOriginalPasswordDoesNotMatchExisting() throws Exception {
        final UserUpdateDto dto = dtoForUpdate();
        dto.setOriginalPassword("test");
        final String msg = "Provided original password does not match.";
        doThrow(new ValidationException(msg)).when(securityUtilsMock)
                                             .verifyCurrentUserPassword(dto.getOriginalPassword());
        final MvcResult result = mockMvc.perform(
                put(BASE_URL + "/current").content(toJson(dto)).contentType(MediaType.APPLICATION_JSON_VALUE))
                                        .andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(result, ErrorInfo.class);
        assertEquals(msg, errorInfo.getMessage());
        verify(userService, never()).update(any());
    }

    @Test
    void updateCurrentSkipsPasswordVerificationWhenNoPasswordIsSpecified() throws Exception {
        final UserUpdateDto dto = dtoForUpdate();
        dto.setPassword(null);
        dto.setOriginalPassword(null);

        mockMvc.perform(
                put(BASE_URL + "/current").content(toJson(dto)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isNoContent());
        verify(securityUtilsMock, never()).verifyCurrentUserPassword(anyString());
        verify(userService).update(user);
    }

    @Test
    void unlockUnlocksUser() throws Exception {
        final String newPassword = "newPassword";

        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.find(user.getUri())).thenReturn(Optional.of(user));
        mockMvc.perform(delete(BASE_URL + extractFragment(user.getUri()) + "/lock")
                .content(newPassword))
               .andExpect(status().isNoContent());
        verify(userService).unlock(user, newPassword);
    }

    @Test
    void unlockReturnsNotFoundForUnknownUserUri() throws Exception {
        final String newPassword = "newPassword";
        final URI uri = Generator.generateUri();

        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(uri);
        when(userService.find(uri)).thenReturn(Optional.empty());
        mockMvc.perform(delete(BASE_URL + extractFragment(uri) + "/lock")
                .content(newPassword))
               .andExpect(status().isNotFound());
        verify(userService, never()).unlock(any(), any());
    }

    @Test
    void enableEnablesUser() throws Exception {
        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.find(user.getUri())).thenReturn(Optional.of(user));
        mockMvc.perform(post(BASE_URL + extractFragment(user.getUri()) + "/status")).andExpect(status().isNoContent());
        verify(userService).enable(user);
    }

    @Test
    void enableUserThrowsNotFoundForUnknownUserUri() throws Exception {
        final URI uri = Generator.generateUri();

        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(uri);
        when(userService.find(uri)).thenReturn(Optional.empty());
        mockMvc.perform(post(BASE_URL + extractFragment(uri) + "/status")).andExpect(status().isNotFound());
        verify(userService, never()).enable(any());
    }

    @Test
    void disableDisablesUser() throws Exception {
        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.find(user.getUri())).thenReturn(Optional.of(user));
        mockMvc.perform(delete(BASE_URL + extractFragment(user.getUri()) + "/status"))
               .andExpect(status().isNoContent());
        verify(userService).disable(user);
    }

    @Test
    void disableThrowsNotFoundForUnknownUserUri() throws Exception {
        final URI uri = Generator.generateUri();

        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(uri);
        when(userService.find(uri)).thenReturn(Optional.empty());
        mockMvc.perform(delete(BASE_URL + extractFragment(uri) + "/status")).andExpect(status().isNotFound());
        verify(userService, never()).disable(any());
    }

    @Test
    void existsChecksForUsernameExistence() throws Exception {
        when(userService.exists(user.getUsername())).thenReturn(true);
        final MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/username").param("username", user.getUsername()))
                                           .andReturn();
        final Boolean result = readValue(mvcResult, Boolean.class);
        assertTrue(result);
    }
}