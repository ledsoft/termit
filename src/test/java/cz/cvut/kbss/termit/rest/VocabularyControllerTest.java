package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
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

import static cz.cvut.kbss.termit.rest.BaseController.ID_QUERY_PARAM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VocabularyControllerTest extends BaseControllerTestRunner {

    @Mock
    private VocabularyRepositoryService serviceMock;

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

        final MvcResult mvcResult = mockMvc.perform(
                post("/vocabularies").content(toJson(vocabulary)).contentType(MediaType.APPLICATION_JSON_VALUE))
                                           .andExpect(status().isCreated()).andReturn();
        verifyLocationEquals("/vocabularies?" + ID_QUERY_PARAM + "=" + vocabulary.getUri().toString(), mvcResult);
    }

    @Test
    void getByIdLoadsVocabularyFromRepository() throws Exception {
        final Vocabulary vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        when(serviceMock.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));

        final MvcResult mvcResult = mockMvc
                .perform(get("/vocabularies/instance").param(ID_QUERY_PARAM, vocabulary.getUri().toString())
                                                      .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();
        final Vocabulary result = readValue(mvcResult, Vocabulary.class);
        assertNotNull(result);
        assertEquals(vocabulary.getUri(), result.getUri());
        assertEquals(vocabulary.getName(), result.getName());
    }

    @Test
    void getByIdReturnsNotFoundForUnknownVocabularyId() throws Exception {
        final String id = Generator.generateUri().toString();
        when(serviceMock.find(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/vocabularies/instance").param(ID_QUERY_PARAM, id)).andExpect(status().isNotFound());
        verify(serviceMock).find(URI.create(id));
    }
}