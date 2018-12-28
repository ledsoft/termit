package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.rest.dto.UserUpdateDto;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.UserService;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.service.IdentifierResolver.extractIdentifierFragment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTestRunner {

    private static final String BASE_URL = "/users";

    @Mock
    private UserService userService;

    @Mock
    private IdentifierResolver idResolverMock;

    @InjectMocks
    private UserController sut;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.user = Generator.generateUserAccount();
        Environment.setCurrentUser(user);
    }

    @Test
    void getAllReturnsAllUsers() throws Exception {
        final List<UserAccount> users = IntStream.range(0, 5).mapToObj(i -> Generator.generateUserAccount())
                                                 .collect(Collectors.toList());
        when(userService.findAll()).thenReturn(users);

        final MvcResult mvcResult = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isOk()).andReturn();
        final List<UserAccount> result = readValue(mvcResult, new TypeReference<List<UserAccount>>() {
        });
        assertEquals(users, result);
    }

    @Test
    void createUserPersistsUser() throws Exception {
        final UserAccount user = Generator.generateUserAccount();
        mockMvc.perform(post(BASE_URL).content(toJson(user)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isCreated());
        verify(userService).persist(user);
    }

    @Test
    void updateCurrentSendsUserUpdateToService() throws Exception {
        final UserUpdateDto dto = dtoForUpdate();

        mockMvc.perform(
                put(BASE_URL + "/current").content(toJson(dto)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isNoContent());
        verify(userService).updateCurrent(dto);
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
    void unlockUnlocksUser() throws Exception {
        final String newPassword = "newPassword";

        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.findRequired(user.getUri())).thenReturn(user);
        mockMvc.perform(delete(BASE_URL + "/" + extractIdentifierFragment(user.getUri()) + "/lock")
                .content(newPassword))
               .andExpect(status().isNoContent());
        verify(userService).unlock(user, newPassword);
    }

    @Test
    void enableEnablesUser() throws Exception {
        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.findRequired(user.getUri())).thenReturn(user);
        mockMvc.perform(post(BASE_URL + "/" + extractIdentifierFragment(user.getUri()) + "/status"))
               .andExpect(status().isNoContent());
        verify(userService).enable(user);
    }

    @Test
    void disableDisablesUser() throws Exception {
        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_USER), any())).thenReturn(user.getUri());
        when(userService.findRequired(user.getUri())).thenReturn(user);
        mockMvc.perform(delete(BASE_URL + "/" + extractIdentifierFragment(user.getUri()) + "/status"))
               .andExpect(status().isNoContent());
        verify(userService).disable(user);
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