package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.DocumentVocabulary;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest extends BaseControllerTestRunner {

    private static final String[] FILE_NAMES = {"mpp.html", "mpp-auxiliary.html", "mpp-definitions.html"};
    private static final String NORMALIZED_NAME = "metropolitan-plan";

    private static final String PATH = "/documents";

    @Mock
    private IdentifierResolver idResolverMock;

    @Mock
    private Configuration configMock;

    @Mock
    private DocumentRepositoryService documentServiceMock;

    @Mock
    private TextAnalysisService textAnalysisServiceMock;

    @InjectMocks
    private DocumentController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisOnSpecifiedFileInDocument() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String fileName = FILE_NAMES[Generator.randomIndex(FILE_NAMES)];
        mockMvc.perform(
                put(PATH + "/" + NORMALIZED_NAME + "/text-analysis").param("namespace", Vocabulary.ONTOLOGY_IRI_termit)
                                                                    .param("file", fileName))
               .andExpect(status().isAccepted());
        verify(textAnalysisServiceMock).analyzeDocument(getFile(doc, fileName), doc);
    }

    private static Document generateTestData() {
        final Document doc = new Document();
        doc.setUri(Generator.generateUri());
        doc.setName("Metropolitan Plan");
        final DocumentVocabulary vocabulary = new DocumentVocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setName("Test Vocabulary");
        vocabulary.setDocument(doc);
        doc.setVocabulary(vocabulary);
        for (String name : FILE_NAMES) {
            final File file = new File();
            file.setFileName(name);
            doc.addFile(file);
        }
        return doc;
    }

    private static File getFile(Document doc, String name) {
        return doc.getFiles().stream().filter(f -> f.getFileName().equals(name)).findFirst()
                  .orElseThrow(() -> new TermItException("File not found."));
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisForAllFilesInDocumentWhenFileNameIsNotSpecified() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        mockMvc.perform(
                put(PATH + "/" + NORMALIZED_NAME + "/text-analysis").param("namespace", Vocabulary.ONTOLOGY_IRI_termit))
               .andExpect(status().isAccepted());
        for (File f : doc.getFiles()) {
            verify(textAnalysisServiceMock).analyzeDocument(f, doc);
        }
    }

    @Test
    void runTextAnalysisThrowsNotFoundExceptionForUnknownDocumentId() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_NAME))).thenReturn(doc.getUri());
        final MvcResult mvcResult = mockMvc.perform(
                put(PATH + "/" + NORMALIZED_NAME + "/text-analysis").param("namespace", Vocabulary.ONTOLOGY_IRI_termit))
                                           .andExpect(status().isNotFound()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString(doc.getUri().toString()));
        verify(textAnalysisServiceMock, never()).analyzeDocument(any(), any());
    }

    @Test
    void runTextAnalysisThrowsNotFoundExceptionForUnknownFileName() throws Exception {
        final Document doc = generateTestData();
        when(idResolverMock.resolveIdentifier(anyString(), eq(NORMALIZED_NAME))).thenReturn(doc.getUri());
        when(documentServiceMock.find(doc.getUri())).thenReturn(Optional.of(doc));
        final String fileName = "unknownFile";
        final MvcResult mvcResult = mockMvc.perform(
                put(PATH + "/" + NORMALIZED_NAME + "/text-analysis").param("namespace", Vocabulary.ONTOLOGY_IRI_termit)
                                                                    .param("file", fileName))
                                           .andExpect(status().isNotFound()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString(fileName));
        verify(textAnalysisServiceMock, never()).analyzeDocument(any(), any());
    }
}