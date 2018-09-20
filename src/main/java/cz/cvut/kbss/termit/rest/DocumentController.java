package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

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

    private Document getDocument(URI id) {
        return documentService.find(id).orElseThrow(() -> NotFoundException.create(Document.class.getSimpleName(), id));
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
                                @RequestParam(name = "namespace") String namespace,
                                @RequestParam(value = "file", required = false) String fileName) {
        final URI docUri = idResolver.resolveIdentifier(namespace, documentName);
        final Document document = getDocument(docUri);
        if (fileName != null) {
            final Optional<File> toAnalyze = resolveFileToAnalyze(document, fileName);
            if (toAnalyze.isPresent()) {
                textAnalysisService.analyzeDocument(toAnalyze.get(), document);
            } else {
                throw new NotFoundException("File " + fileName + " not found in document " + document + ".");
            }
        } else {
            for (File f : document.getFiles()) {
                textAnalysisService.analyzeDocument(f, document);
            }
        }
    }

    private static Optional<File> resolveFileToAnalyze(Document document, String fileName) {
        return document.getFiles().stream().filter(f -> f.getFileName().equals(fileName)).findAny();
    }
}
