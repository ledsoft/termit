package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyControllerTest extends BaseControllerTestRunner {

    @Mock
    private VocabularyRepositoryService serviceMock;

    @Mock
    private IdentifierResolver idResolverMock;

    @InjectMocks
    private VocabularyController sut;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.user = Generator.generateUserWithId();
    }

    @Test
    void getAllReturnsAllExistingVocabularies() throws Exception {
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> {
            final Vocabulary vocab = Generator.generateVocabulary();
            vocab.setAuthor(user);
            vocab.setDateCreated(new Date());
            vocab.setUri(Generator.generateUri());
            return vocab;
        }).collect(Collectors.toList());
        when(serviceMock.findAll()).thenReturn(vocabularies);

        final MvcResult mvcResult = mockMvc.perform(get("/vocabularies")).andExpect(status().isOk()).andReturn();
        final List<Vocabulary> result = readValue(mvcResult, new TypeReference<List<Vocabulary>>() {
        });
        assertEquals(vocabularies.size(), result.size());
        for (Vocabulary voc : vocabularies) {
            assertTrue(result.stream().anyMatch(v -> v.getUri().equals(voc.getUri())));
        }
    }

    @Test
    void createVocabularyPersistsSpecifiedVocabulary() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());

        mockMvc.perform(post("/vocabularies").content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isCreated());
        final ArgumentCaptor<Vocabulary> captor = ArgumentCaptor.forClass(Vocabulary.class);
        verify(serviceMock).persist(captor.capture());
        assertEquals(vocabulary.getUri(), captor.getValue().getUri());
    }

    @Test
    void createVocabularyReturnsResponseWithLocationHeader() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = Environment.extractFragment(vocabulary.getUri());
        when(idResolverMock.extractIdentifierFragment(vocabulary.getUri())).thenReturn(fragment);

        final MvcResult mvcResult = mockMvc.perform(
                post("/vocabularies").content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isCreated()).andReturn();
        verifyLocationEquals("/vocabularies/" + fragment, mvcResult);
    }

    @Test
    void getByIdLoadsVocabularyFromRepository() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = Environment.extractFragment(vocabulary.getUri());
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment.substring(1)))
                .thenReturn(vocabulary.getUri());
        when(serviceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));

        final MvcResult mvcResult = mockMvc
                .perform(get("/vocabularies" + fragment).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
        final Vocabulary result = readValue(mvcResult, Vocabulary.class);
        assertNotNull(result);
        assertEquals(vocabulary.getUri(), result.getUri());
        assertEquals(vocabulary.getName(), result.getName());
    }

    @Test
    void getByIdReturnsNotFoundForUnknownVocabularyId() throws Exception {
        final URI unknownUri = Generator.generateUri();
        final String fragment = Environment.extractFragment(unknownUri);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment.substring(1)))
                .thenReturn(unknownUri);
        when(serviceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/vocabularies" + fragment)).andExpect(status().isNotFound());
        verify(serviceMock).find(unknownUri);
    }

    @Test
    void getByIdUsesSpecifiedNamespaceInsteadOfDefaultOneForResolvingIdentifier() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = Environment.extractFragment(vocabulary.getUri()).substring(1);
        final String namespace = vocabulary.getUri().toString()
                                           .substring(0, vocabulary.getUri().toString().lastIndexOf('/'));
        when(idResolverMock.resolveIdentifier(namespace, fragment)).thenReturn(vocabulary.getUri());
        when(serviceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));

        final MvcResult mvcResult = mockMvc.perform(
                get("/vocabularies/" + fragment).accept(MediaType.APPLICATION_JSON_VALUE).param("namespace", namespace))
                                           .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus());
        verify(idResolverMock).resolveIdentifier(namespace, fragment);
    }

    @Test
    void generateIdentifierReturnsIdentifierGeneratedForSpecifiedName() throws Exception {
        final String name = "Metropolitní plán";
        final URI uri = URI.create(cz.cvut.kbss.termit.util.Vocabulary.ONTOLOGY_IRI_termit + "/" +
                IdentifierResolver.normalize(name));
        when(idResolverMock.generateIdentifier(any(ConfigParam.class), eq(name))).thenReturn(uri);
        final MvcResult mvcResult = mockMvc.perform(get("/vocabularies/identifier").param("name", name)).andReturn();
        assertEquals(uri.toString(), readValue(mvcResult, String.class));
        verify(idResolverMock).generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, name);
    }
}