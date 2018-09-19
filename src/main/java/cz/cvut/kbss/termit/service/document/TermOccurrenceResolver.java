package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;

import java.io.InputStream;
import java.util.List;

/**
 * Base class for resolving term occurrences in an annotated document.
 */
public abstract class TermOccurrenceResolver {

    protected final TermRepositoryService termService;

    protected TermOccurrenceResolver(TermRepositoryService termService) {
        this.termService = termService;
    }

    /**
     * Finds term occurrences in the input stream.
     *
     * @param input  Data containing term occurrence identification
     * @param source Original source of the data. Used for term occurrence generation
     * @return List of term occurrences identified in the input
     */
    public abstract List<TermOccurrence> findTermOccurrences(InputStream input, File source);

    /**
     * Checks whether this resolver supports the specified source file type.
     *
     * @param source File to check
     * @return Support status
     */
    public abstract boolean supports(File source);

    protected TermOccurrence createOccurrence(Term term) {
        final TermOccurrence occurrence = new TermOccurrence();
        occurrence.setTerm(term);
        return occurrence;
    }
}
