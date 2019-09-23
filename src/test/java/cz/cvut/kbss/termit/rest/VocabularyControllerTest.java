package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.VocabularyImportException;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.VocabularyService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/vocabularies";
    private static final String NAMESPACE = "http://onto.fel.cvut.cz/ontologies/termit/vocabularies/";
    private static final String FRAGMENT = "test";
    private static final URI VOCABULARY_URI = URI.create(NAMESPACE + FRAGMENT);

    @Mock
    private VocabularyService serviceMock;

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private Configuration configMock;

    @InjectMocks
    private VocabularyController sut;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        this.user = Generator.generateUserWithId();
        when(configMock.get(ConfigParam.NAMESPACE_VOCABULARY)).thenReturn(Environment.BASE_URI + "/");
    }

    @Test
    void getAllReturnsAllExistingVocabularies() throws Exception {
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> {
            final Vocabulary vocab = Generator.generateVocabulary();
            vocab.setAuthor(user);
            vocab.setCreated(new Date());
            vocab.setUri(Generator.generateUri());
            return vocab;
        }).collect(Collectors.toList());
        when(serviceMock.findAll()).thenReturn(vocabularies);

        final MvcResult mvcResult = mockMvc.perform(get(PATH)).andExpect(status().isOk()).andReturn();
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

        mockMvc.perform(post(PATH).content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
               .andExpect(status().isCreated());
        final ArgumentCaptor<Vocabulary> captor = ArgumentCaptor.forClass(Vocabulary.class);
        verify(serviceMock).persist(captor.capture());
        assertEquals(vocabulary.getUri(), captor.getValue().getUri());
    }

    @Test
    void createVocabularyReturnsResponseWithLocationHeader() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = IdentifierResolver.extractIdentifierFragment(vocabulary.getUri());

        final MvcResult mvcResult = mockMvc.perform(
                post(PATH).content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isCreated()).andReturn();
        verifyLocationEquals(PATH + "/" + fragment, mvcResult);
    }

    @Test
    void getByIdLoadsVocabularyFromRepository() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = IdentifierResolver.extractIdentifierFragment(vocabulary.getUri());
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, fragment))
                .thenReturn(vocabulary.getUri());
        when(serviceMock.findRequired(vocabulary.getUri())).thenReturn(vocabulary);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + fragment).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
        final Vocabulary result = readValue(mvcResult, Vocabulary.class);
        assertNotNull(result);
        assertEquals(vocabulary.getUri(), result.getUri());
        assertEquals(vocabulary.getLabel(), result.getLabel());
    }

    @Test
    void getByIdUsesSpecifiedNamespaceInsteadOfDefaultOneForResolvingIdentifier() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final String fragment = IdentifierResolver.extractIdentifierFragment(vocabulary.getUri()).substring(1);
        final String namespace = vocabulary.getUri().toString()
                                           .substring(0, vocabulary.getUri().toString().lastIndexOf('/'));
        when(idResolverMock.resolveIdentifier(namespace, fragment)).thenReturn(vocabulary.getUri());
        when(serviceMock.findRequired(vocabulary.getUri())).thenReturn(vocabulary);

        final MvcResult mvcResult = mockMvc.perform(
                get(PATH + "/" + fragment).accept(MediaType.APPLICATION_JSON_VALUE)
                                          .param(QueryParams.NAMESPACE, namespace))
                                           .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus());
        verify(idResolverMock).resolveIdentifier(namespace, fragment);
    }

    @Test
    void generateIdentifierReturnsIdentifierGeneratedForSpecifiedName() throws Exception {
        final String name = "Metropolitní plán";
        final URI uri = URI.create(Environment.BASE_URI + "/" + IdentifierResolver.normalize(name));
        when(serviceMock.generateIdentifier(name)).thenReturn(uri);
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/identifier").param("name", name)).andReturn();
        assertEquals(uri.toString(), readValue(mvcResult, String.class));
        verify(serviceMock).generateIdentifier(name);
    }

    @Test
    void createVocabularyReturnsResponseWithLocationSpecifyingNamespaceWhenItIsDifferentFromConfiguredOne()
            throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(VOCABULARY_URI);
        final String configuredNamespace = "http://kbss.felk.cvut.cz/ontologies/termit/vocabularies/";
        when(configMock.get(ConfigParam.NAMESPACE_VOCABULARY)).thenReturn(configuredNamespace);
        final MvcResult mvcResult = mockMvc.perform(
                post(PATH).content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isCreated()).andReturn();
        final String location = mvcResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location, containsString(QueryParams.NAMESPACE + "=" + NAMESPACE));
    }

    @Test
    void updateVocabularyUpdatesVocabularyUpdateToService() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(user);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(eq(ConfigParam.NAMESPACE_VOCABULARY), any())).thenReturn(VOCABULARY_URI);
        when(serviceMock.exists(VOCABULARY_URI)).thenReturn(true);
        mockMvc.perform(put(PATH + "/test").contentType(MediaType.APPLICATION_JSON_VALUE).content(toJson(vocabulary)))
               .andExpect(status().isNoContent());
        verify(serviceMock).update(vocabulary);
    }

    @Test
    void updateVocabularyThrowsValidationExceptionWhenVocabularyUriDiffersFromRequestBasedUri() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(user);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, FRAGMENT)).thenReturn(VOCABULARY_URI);
        when(serviceMock.exists(VOCABULARY_URI)).thenReturn(false);
        final MvcResult mvcResult = mockMvc
                .perform(put(PATH + "/" + FRAGMENT).contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content(toJson(vocabulary)))
                .andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertNotNull(errorInfo);
        assertThat(errorInfo.getMessage(), containsString("does not match the ID of the specified entity"));
        verify(serviceMock, never()).update(any());
    }

    @Test
    void updateVocabularyThrowsVocabularyImportExceptionWithMessageIdWhenServiceThrowsException() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(user);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, FRAGMENT)).thenReturn(VOCABULARY_URI);
        final String errorMsg = "Error message";
        final String errorMsgId = "message.id";
        when(serviceMock.update(any())).thenThrow(new VocabularyImportException(errorMsg, errorMsgId));

        final MvcResult mvcResult = mockMvc
                .perform(put(PATH + "/" + FRAGMENT).contentType(MediaType.APPLICATION_JSON_VALUE)
                                                   .content(toJson(vocabulary)))
                .andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertNotNull(errorInfo);
        assertEquals(errorMsg, errorInfo.getMessage());
        assertEquals(errorMsgId, errorInfo.getMessageId());
    }

    @Test
    void getTransitiveImportsReturnsCollectionOfImportIdentifiersRetrievedFromService() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(user);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, FRAGMENT)).thenReturn(VOCABULARY_URI);
        final Set<URI> imports = IntStream.range(0, 5).mapToObj(i -> Generator.generateUri())
                                          .collect(Collectors.toSet());
        when(serviceMock.getRequiredReference(VOCABULARY_URI)).thenReturn(vocabulary);
        when(serviceMock.getTransitivelyImportedVocabularies(vocabulary)).thenReturn(imports);

        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + FRAGMENT + "/imports")).andExpect(status().isOk())
                                           .andReturn();
        final Set<URI> result = readValue(mvcResult, new TypeReference<Set<URI>>() {
        });
        assertEquals(imports, result);
        verify(serviceMock).getRequiredReference(VOCABULARY_URI);
        verify(serviceMock).getTransitivelyImportedVocabularies(vocabulary);
    }

    @Test
    void getTransitiveImportsReturnsEmptyCollectionWhenNoImportsAreFoundForVocabulary() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setAuthor(user);
        vocabulary.setCreated(new Date());
        vocabulary.setUri(VOCABULARY_URI);
        when(idResolverMock.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, FRAGMENT)).thenReturn(VOCABULARY_URI);
        when(serviceMock.getRequiredReference(VOCABULARY_URI)).thenReturn(vocabulary);
        when(serviceMock.getTransitivelyImportedVocabularies(vocabulary)).thenReturn(Collections.emptySet());

        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + FRAGMENT + "/imports")).andExpect(status().isOk())
                                           .andReturn();
        final Set<URI> result = readValue(mvcResult, new TypeReference<Set<URI>>() {
        });
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(serviceMock).getRequiredReference(VOCABULARY_URI);
        verify(serviceMock).getTransitivelyImportedVocabularies(vocabulary);
    }
}
