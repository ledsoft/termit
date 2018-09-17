package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.TextAnalysisService;
import cz.cvut.kbss.termit.service.repository.DocumentRepositoryService;
import cz.cvut.kbss.termit.util.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
     * @param fileName     Name of the file to analyze. Optional
     * @param namespace    Namespace used for document identifier resolution
     */
    @RequestMapping(value = "/{document}/text-analysis", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runTextAnalysis(@PathVariable("document") String documentName,
                                @RequestParam(value = "file", required = false) String fileName,
                                @RequestParam(name = "namespace", required = false) String namespace) {

    }
}
