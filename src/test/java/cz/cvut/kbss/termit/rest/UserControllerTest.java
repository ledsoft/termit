package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.repository.UserRepositoryService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTestRunner {

    private static final String BASE_URL = "/users";

    @Mock
    private UserRepositoryService userService;

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
}