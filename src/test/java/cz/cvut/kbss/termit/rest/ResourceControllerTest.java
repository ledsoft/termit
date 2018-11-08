package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/resources";
    private static final String IRI_PARAM = "iri";

    @Mock
    private ResourceRepositoryService resourceServiceMock;

    @Mock
    private SecurityUtils securityUtilsMock;

    @InjectMocks
    private ResourceController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @AfterEach
    void tearDown() {
        Environment.resetCurrentUser();
    }

    @Test
    void getTermsReturnsTermsAssignedToResourceWithSpecifiedIri() throws Exception {
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.find(resource.getUri())).thenReturn(Optional.of(resource));
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(resourceServiceMock.findTerms(resource)).thenReturn(terms);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/resource/terms").param(IRI_PARAM, resource.getUri().toString()))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
    }

    @Test
    void getTermsThrowsNotFoundExceptionWhenResourceDoesNotExist() throws Exception {
        when(resourceServiceMock.find(any())).thenReturn(Optional.empty());
        mockMvc.perform(get(PATH + "/resource/terms").param(IRI_PARAM, Generator.generateUri().toString()))
               .andExpect(status().isNotFound());
        verify(resourceServiceMock, never()).findTerms(any());
    }

    @Test
    void getRelatedResourcesReturnsResourcesRelatedToResourceWithSpecifiedIri() throws Exception {
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.find(resource.getUri())).thenReturn(Optional.of(resource));
        final List<Resource> related = IntStream.range(0, 5).mapToObj(i -> Generator.generateResourceWithId()).collect(
                Collectors.toList());
        when(resourceServiceMock.findRelated(resource)).thenReturn(related);
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/resource/related").param(IRI_PARAM, resource.getUri().toString()))
                .andExpect(status().isOk()).andReturn();
        final List<Resource> result = readValue(mvcResult, new TypeReference<List<Resource>>() {
        });
        assertEquals(related, result);
    }

    @Test
    void getRelatedResourcesThrowsNotFoundExceptionWhenResourceDoesNotExist() throws Exception {
        when(resourceServiceMock.find(any())).thenReturn(Optional.empty());
        mockMvc.perform(get(PATH + "/resource/related").param(IRI_PARAM, Generator.generateUri().toString()))
               .andExpect(status().isNotFound());
        verify(resourceServiceMock, never()).findRelated(any());
    }

    @Test
    void getRelatedResourcesReturnsResourcesWithAuthorInformationWhenUserIsAuthenticated() throws Exception {
        final User author = Generator.generateUserWithId();
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.find(resource.getUri())).thenReturn(Optional.of(resource));
        final List<Resource> related = generateRelatedResources(author);
        when(resourceServiceMock.findRelated(resource)).thenReturn(related);
        when(securityUtilsMock.isAuthenticated()).thenReturn(true);
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/resource/related").param(IRI_PARAM, resource.getUri().toString()))
                .andExpect(status().isOk()).andReturn();
        final List<Resource> result = readValue(mvcResult, new TypeReference<List<Resource>>() {
        });
        assertEquals(related.size(), result.size());
        result.forEach(r -> {
            assertEquals(author, r.getAuthor());
            assertNotNull(r.getDateCreated());
        });
    }

    private List<Resource> generateRelatedResources(User author) {
        return IntStream.range(0, 5).mapToObj(i -> {
            final Resource r = Generator.generateResourceWithId();
            r.setAuthor(author);
            r.setDateCreated(new Date());
            return r;
        }).collect(Collectors.toList());
    }

    @Test
    void getRelatedResourcesReturnsResourcesWithoutAuthorInformationWhenAnonymousRequestInvokesEndpoint()
            throws Exception {
        final User author = Generator.generateUserWithId();
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.find(resource.getUri())).thenReturn(Optional.of(resource));
        final List<Resource> related = generateRelatedResources(author);
        when(resourceServiceMock.findRelated(resource)).thenReturn(related);
        when(securityUtilsMock.isAuthenticated()).thenReturn(false);
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/resource/related").param(IRI_PARAM, resource.getUri().toString()))
                .andExpect(status().isOk()).andReturn();
        final List<Resource> result = readValue(mvcResult, new TypeReference<List<Resource>>() {
        });
        assertEquals(related.size(), result.size());
        result.forEach(r -> {
            assertNull(r.getAuthor());
            assertNotNull(r.getDateCreated());
        });
    }
}