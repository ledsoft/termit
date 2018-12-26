package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.rest.handler.ErrorInfo;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.ResourceService;
import cz.cvut.kbss.termit.service.document.util.TypeAwareFileSystemResource;
import cz.cvut.kbss.termit.service.security.SecurityUtils;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants.QueryParams;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.util.ConfigParam.NAMESPACE_RESOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/resources";
    private static final String IRI_PARAM = "iri";

    private static final String RESOURCE_NAME = "test-resource";
    private static final String RESOURCE_NAMESPACE = Vocabulary.ONTOLOGY_IRI_termit + "/";
    private static final String HTML_CONTENT = "<html><head><title>Test</title></head><body>test</body></html>";

    @Mock
    private ResourceService resourceServiceMock;

    @Mock
    private Configuration configMock;

    @Mock
    private IdentifierResolver identifierResolverMock;

    @Mock
    private SecurityUtils securityUtilsMock;

    @InjectMocks
    private ResourceController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
        when(configMock.get(NAMESPACE_RESOURCE)).thenReturn(RESOURCE_NAMESPACE);
    }

    @AfterEach
    void tearDown() {
        Environment.resetCurrentUser();
    }

    @Test
    void getTermsForNKODReturnsTermsAssignedToResourceWithSpecifiedIri() throws Exception {
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.findRequired(resource.getUri())).thenReturn(resource);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(resourceServiceMock.findTags(resource)).thenReturn(terms);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/resource/terms").param(IRI_PARAM, resource.getUri().toString()))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(resourceServiceMock).findTags(resource);
    }

    @Test
    void getTermsReturnsTermsAssignedToResource() throws Exception {
        final URI resourceUri = URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME);
        final Resource resource = Generator.generateResource();
        resource.setUri(resourceUri);
        resource.setName(RESOURCE_NAME);
        when(identifierResolverMock.resolveIdentifier(RESOURCE_NAMESPACE, RESOURCE_NAME)).thenReturn(resourceUri);
        when(resourceServiceMock.findRequired(resourceUri)).thenReturn(resource);
        final List<Term> terms = IntStream.range(0, 5).mapToObj(i -> Generator.generateTermWithId())
                                          .collect(Collectors.toList());
        when(resourceServiceMock.findTags(resource)).thenReturn(terms);
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + RESOURCE_NAME + "/terms").param(QueryParams.NAMESPACE, RESOURCE_NAMESPACE))
                .andExpect(status().isOk()).andReturn();
        final List<Term> result = readValue(mvcResult, new TypeReference<List<Term>>() {
        });
        assertEquals(terms, result);
        verify(resourceServiceMock).findTags(resource);
    }

    @Test
    void getRelatedResourcesReturnsResourcesRelatedToResourceWithSpecifiedIri() throws Exception {
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.findRequired(resource.getUri())).thenReturn(resource);
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
    void getRelatedResourcesReturnsResourcesWithAuthorInformationWhenUserIsAuthenticated() throws Exception {
        final User author = Generator.generateUserWithId();
        final Resource resource = Generator.generateResourceWithId();
        when(resourceServiceMock.findRequired(resource.getUri())).thenReturn(resource);
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
    void getResourceRetrievesResourceByDefaultNamespaceAndSpecifiedNormalizedName() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        final URI resourceId = URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME);
        resource.setUri(resourceId);
        when(identifierResolverMock.resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME))
                .thenReturn(resourceId);
        when(resourceServiceMock.findRequired(resourceId)).thenReturn(resource);
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/" + RESOURCE_NAME)).andExpect(status().isOk())
                                           .andReturn();
        final Resource result = readValue(mvcResult, Resource.class);
        assertEquals(resource, result);
        verify(resourceServiceMock).findRequired(resourceId);
        verify(identifierResolverMock).resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME);
    }

    @Test
    void getResourceUsesSpecifiedNamespaceForResourceRetrieval() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        final URI resourceId = URI.create(RESOURCE_NAMESPACE + "/" + RESOURCE_NAME);
        resource.setUri(resourceId);
        when(identifierResolverMock.resolveIdentifier(RESOURCE_NAMESPACE, RESOURCE_NAME)).thenReturn(resourceId);
        when(resourceServiceMock.findRequired(resourceId)).thenReturn(resource);
        final MvcResult mvcResult =
                mockMvc.perform(get(PATH + "/" + RESOURCE_NAME).param("namespace", RESOURCE_NAMESPACE))
                       .andExpect(status().isOk())
                       .andReturn();
        final Resource result = readValue(mvcResult, Resource.class);
        assertEquals(resource, result);
        verify(resourceServiceMock).findRequired(resourceId);
        verify(identifierResolverMock).resolveIdentifier(RESOURCE_NAMESPACE, RESOURCE_NAME);
    }

    @Test
    void createResourcePassesNewResourceToService() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        resource.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        mockMvc.perform(post(PATH).content(toJson(resource)).contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isCreated());
        verify(resourceServiceMock).persist(resource);
    }

    @Test
    void createResourceReturnsLocationHeaderOnSuccess() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        resource.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        final MvcResult mvcResult = mockMvc
                .perform(post(PATH).content(toJson(resource)).contentType(MediaType.APPLICATION_JSON)).andReturn();
        verifyLocationEquals(PATH + "/" + RESOURCE_NAME, mvcResult);
    }

    @Test
    void createResourceReturnsLocationHeaderWithNamespaceParameterWhenItDiffersFromDefault() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        final String namespace = "http://onto.fel.cvut.cz/ontologies/test/termit/resources/";
        resource.setUri(URI.create(namespace + RESOURCE_NAME));
        final MvcResult mvcResult = mockMvc
                .perform(post(PATH).content(toJson(resource)).contentType(MediaType.APPLICATION_JSON)).andReturn();
        verifyLocationEquals(PATH + "/" + RESOURCE_NAME, mvcResult);
        final String location = mvcResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location, containsString(QueryParams.NAMESPACE + "=" + namespace));
    }

    @Test
    void updateResourcePassesUpdateDataToService() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        resource.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        when(resourceServiceMock.exists(resource.getUri())).thenReturn(true);
        when(identifierResolverMock.resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME)).thenReturn(resource.getUri());
        mockMvc.perform(
                put(PATH + "/" + RESOURCE_NAME).content(toJson(resource)).contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNoContent());
        verify(identifierResolverMock).resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME);
        verify(resourceServiceMock).update(resource);
    }

    @Test
    void updateResourceThrowsConflictExceptionWhenRequestUrlIdentifierDiffersFromEntityIdentifier() throws Exception {
        final Resource resource = Generator.generateResourceWithId();
        resource.setName(RESOURCE_NAME);
        when(resourceServiceMock.exists(resource.getUri())).thenReturn(true);
        when(identifierResolverMock.resolveIdentifier(RESOURCE_NAMESPACE, RESOURCE_NAME))
                .thenReturn(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        final MvcResult mvcResult = mockMvc.perform(
                put(PATH + "/" + RESOURCE_NAME).content(toJson(resource)).contentType(MediaType.APPLICATION_JSON)
                                               .param(QueryParams.NAMESPACE, RESOURCE_NAMESPACE))
                                           .andExpect(status().isConflict()).andReturn();
        final ErrorInfo errorInfo = readValue(mvcResult, ErrorInfo.class);
        assertThat(errorInfo.getMessage(), containsString("does not match the ID of the specified entity"));
    }

    @Test
    void setTermsPassesNewTermUrisToService() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        resource.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        when(identifierResolverMock.resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME)).thenReturn(resource.getUri());
        when(resourceServiceMock.findRequired(resource.getUri())).thenReturn(resource);
        final List<URI> uris = IntStream.range(0, 5).mapToObj(i -> Generator.generateUri())
                                        .collect(Collectors.toList());
        mockMvc.perform(put(PATH + "/" + RESOURCE_NAME + "/terms").content(toJson(uris))
                                                                  .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNoContent());
        verify(resourceServiceMock).findRequired(resource.getUri());
        verify(resourceServiceMock).setTags(resource, uris);
    }

    @Test
    void getAllRetrievesResourcesFromUnderlyingService() throws Exception {
        final List<Resource> resources = IntStream.range(0, 5).mapToObj(i -> Generator.generateResourceWithId())
                                                  .collect(Collectors.toList());
        when(resourceServiceMock.findAll()).thenReturn(resources);
        final MvcResult mvcResult = mockMvc.perform(get(PATH)).andReturn();
        final List<Resource> result = readValue(mvcResult, new TypeReference<List<Resource>>() {
        });
        verify(resourceServiceMock).findAll();
        assertEquals(resources, result);
    }

    @Test
    void removeResourceRemovesResourceViaService() throws Exception {
        final Resource resource = Generator.generateResource();
        resource.setName(RESOURCE_NAME);
        resource.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        when(identifierResolverMock.resolveIdentifier(NAMESPACE_RESOURCE, RESOURCE_NAME)).thenReturn(resource.getUri());
        when(resourceServiceMock.findRequired(resource.getUri())).thenReturn(resource);
        mockMvc.perform(delete(PATH + "/" + RESOURCE_NAME)).andExpect(status().isNoContent());
        verify(resourceServiceMock).findRequired(resource.getUri());
        verify(resourceServiceMock).remove(resource);
    }

    @Test
    void createResourceSupportsSubtypesOfResource() throws Exception {
        final Document doc = new Document();
        doc.setName(RESOURCE_NAME);
        doc.setUri(URI.create(RESOURCE_NAMESPACE + RESOURCE_NAME));
        final File file = new File();
        file.setName("test-file.html");
        file.setUri(URI.create(RESOURCE_NAMESPACE + file.getName()));
        doc.setFiles(Collections.singleton(file));
        mockMvc.perform(post(PATH).content(toJsonLd(doc)).contentType(JsonLd.MEDIA_TYPE))
               .andExpect(status().isCreated());
        final ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(resourceServiceMock).persist(captor.capture());
        assertEquals(doc, captor.getValue());
        assertEquals(doc.getFiles(), captor.getValue().getFiles());
    }

    @Test
    void getContentReturnsContentOfRequestedFile() throws Exception {
        final File file = new File();
        final String fileName = "mpp-3.3.html";
        file.setUri(URI.create(RESOURCE_NAMESPACE + fileName));
        file.setName(fileName);
        when(identifierResolverMock.resolveIdentifier(any(ConfigParam.class), eq(fileName)))
                .thenReturn(file.getUri());
        when(resourceServiceMock.findRequired(file.getUri())).thenReturn(file);
        final java.io.File content = createTemporaryHtmlFile();
        when(resourceServiceMock.getContent(file))
                .thenReturn(new TypeAwareFileSystemResource(content, MediaType.TEXT_HTML_VALUE));
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + "/" + fileName + "/content"))
                .andExpect(status().isOk()).andReturn();
        final String resultContent = mvcResult.getResponse().getContentAsString();
        assertEquals(HTML_CONTENT, resultContent);
        assertEquals(MediaType.TEXT_HTML_VALUE, mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_TYPE));
    }

    private static java.io.File createTemporaryHtmlFile() throws Exception {
        final java.io.File file = Files.createTempFile("document", ".html").toFile();
        file.deleteOnExit();
        Files.write(file.toPath(), HTML_CONTENT.getBytes());
        return file;
    }

    @Test
    void saveContentSavesContentViaServiceAndReturnsNoContentStatus() throws Exception {
        final File file = new File();
        final String fileName = "mpp-3.3.html";
        file.setUri(URI.create(RESOURCE_NAMESPACE + fileName));
        file.setName(fileName);
        when(identifierResolverMock.resolveIdentifier(any(ConfigParam.class), eq(fileName)))
                .thenReturn(file.getUri());
        when(resourceServiceMock.findRequired(file.getUri())).thenReturn(file);

        final java.io.File attachment = createTemporaryHtmlFile();
        final MockMultipartFile upload = new MockMultipartFile("file", file.getName(), MediaType.TEXT_HTML_VALUE,
                Files.readAllBytes(attachment.toPath())
        );
        mockMvc.perform(multipart(PATH + "/" + fileName + "/content").file(upload)).andExpect(status().isNoContent());
        verify(resourceServiceMock).saveContent(eq(file), any(InputStream.class));
    }

    @Test
    void runTextAnalysisInvokesTextAnalysisOnSpecifiedResource() throws Exception {
        final File file = new File();
        final String fileName = "mpp-3.3.html";
        file.setUri(URI.create(RESOURCE_NAMESPACE + fileName));
        file.setName(fileName);
        when(identifierResolverMock.resolveIdentifier(RESOURCE_NAMESPACE, fileName)).thenReturn(file.getUri());
        when(resourceServiceMock.findRequired(file.getUri())).thenReturn(file);
        mockMvc.perform(put(PATH + "/" + fileName + "/text-analysis").param(QueryParams.NAMESPACE, RESOURCE_NAMESPACE))
               .andExpect(status().isAccepted());
        verify(resourceServiceMock).runTextAnalysis(file);
    }
}