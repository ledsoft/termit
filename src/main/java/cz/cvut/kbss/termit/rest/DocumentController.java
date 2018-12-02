package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.exception.TermItException;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.DocumentManager;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class DocumentController extends BaseController {

    private final DocumentRepositoryService documentService;

    private final DocumentManager documentManager;

    private final TextAnalysisService textAnalysisService;

    public DocumentController(IdentifierResolver idResolver, Configuration config,
                              DocumentRepositoryService documentService,
                              DocumentManager documentManager,
                              TextAnalysisService textAnalysisService) {
        super(idResolver, config);
        this.documentService = documentService;
        this.documentManager = documentManager;
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
        final URI id = resolveIdentifier(namespace, fragment, ConfigParam.NAMESPACE_DOCUMENT);
        return documentService.find(id).orElseThrow(() -> NotFoundException.create(Document.class.getSimpleName(), id));
    }

    @RequestMapping(value = "/{fragment}/content", method = RequestMethod.GET)
    public ResponseEntity<Resource> getFileContent(@PathVariable("fragment") String fragment,
                                                   @RequestParam(name = "namespace", required = false) String namespace,
                                                   @RequestParam(name = "file") String fileName) {
        final Document document = getById(fragment, namespace);
        final File file = resolveFileFromName(document, fileName);
        try {
            final Resource resource = documentManager.getAsResource(document, file);
            return ResponseEntity.ok()
                                 .contentLength(resource.contentLength())
                                 .contentType(MediaType.parseMediaType(documentManager.getMediaType(document, file)
                                                                                      .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                                 .body(resource);
        } catch (IOException e) {
            throw new TermItException("Unable to load file " + file, e);
        }
    }

    @RequestMapping(value = "/{fragment}/content", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFileContent(@PathVariable("fragment") String fragment,
                                  @RequestParam(name = "namespace", required = false) String namespace,
                                  @RequestParam(name = "file") MultipartFile attachment) {
        final Document document = getById(fragment, namespace);
        String fileName = attachment.getOriginalFilename();
        final File file = resolveFileFromName(document, fileName);
        try {
            documentManager.createBackup(document, file);
            documentManager.saveFileContent(document, file, attachment.getInputStream());
        } catch (IOException e) {
            throw new TermItException("Unable to read file (fileName=\"" + fileName + "\") content from request", e);
        }
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
        return document.getFile(fileName).orElseThrow(
                () -> new NotFoundException("File " + fileName + " not found in document " + document + "."));
    }
}
