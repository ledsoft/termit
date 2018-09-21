package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController extends BaseController {

    private final DocumentRepositoryService documentService;

    private final TextAnalysisService textAnalysisService;

    public DocumentController(IdentifierResolver idResolver, Configuration config,
                              DocumentRepositoryService documentService,
                              TextAnalysisService textAnalysisService) {
        super(idResolver, config);
        this.documentService = documentService;
        this.textAnalysisService = textAnalysisService;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Document> getAll() {
        return documentService.findAll();
    }

    @RequestMapping(value = "/{fragment}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE,
            JsonLd.MEDIA_TYPE})
    public Document getById(@PathVariable("fragment") String fragment,
                            @RequestParam(name = "namespace", required = false) String namespace) {
        final URI id;
        if (namespace != null) {
            id = idResolver.resolveIdentifier(namespace, fragment);
        } else {
            id = idResolver.resolveIdentifier(ConfigParam.NAMESPACE_DOCUMENT, fragment);
        }
        return documentService.find(id).orElseThrow(() -> NotFoundException.create(Document.class.getSimpleName(), id));
    }

    @RequestMapping(value = "/{fragment}/content", method = RequestMethod.GET)
    public ResponseEntity<Resource> getFileContent(@PathVariable("fragment") String fragment,
                                                   @RequestParam(name = "namespace", required = false) String namespace,
                                                   @RequestParam(name = "file") String fileName) {
        final Document document = getById(fragment, namespace);
        final File file = resolveFileFromName(document, fileName);
        try {
            final java.io.File content = documentService.resolveFile(document, file);
            final FileSystemResource resource = new FileSystemResource(content);
            return ResponseEntity.ok()
                                 .contentLength(resource.contentLength())
                                 .contentType(resolveFileMediaType(content))
                                 .body(resource);
        } catch (IOException e) {
            throw new TermItException("Unable to load file " + file, e);
        }
    }

    private static MediaType resolveFileMediaType(java.io.File file) throws IOException {
        final String type = Files.probeContentType(file.toPath());
        return type != null ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * Runs text analysis on the specified document.
     * <p>
     * If file name is specified, text analysis is run only on the specified file. Otherwise, all files registered under
     * the document are analyzed.
     * <p>
     * Note that the text analysis invocation is asynchronous, so this method returns immediately after invoking the
     * text analysis with status {@link HttpStatus#ACCEPTED}.
     *
     * @param documentName Normalized name used to identify document
     * @param namespace    Namespace used for document identifier resolution. It should be derived from URI of the
     *                     vocabulary with which the document is associated
     * @param fileName     Name of the file to analyze. Optional
     */
    @RequestMapping(value = "/{document}/text-analysis", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runTextAnalysis(@PathVariable("document") String documentName,
                                @RequestParam(name = "namespace", required = false) String namespace,
                                @RequestParam(value = "file", required = false) String fileName) {
        final Document document = getById(documentName, namespace);
        if (fileName != null) {
            File toAnalyze = resolveFileFromName(document, fileName);
            textAnalysisService.analyzeDocument(toAnalyze, document);
        } else {
            for (File f : document.getFiles()) {
                textAnalysisService.analyzeDocument(f, document);
            }
        }
    }

    private static File resolveFileFromName(Document document, String fileName) {
        return document.getFiles().stream().filter(f -> f.getFileName().equals(fileName)).findAny().orElseThrow(
                () -> new NotFoundException("File " + fileName + " not found in document " + document + "."));
    }
}
