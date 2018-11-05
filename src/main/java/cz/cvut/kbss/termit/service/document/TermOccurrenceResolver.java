package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.Vocabulary;
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
     * Parses the specified input into some abstract representation from which new terms and term occurrences can be
     * extracted.
     * <p>
     * Note that this method has to be called before calling {@link #findTermOccurrences()} and {@link
     * #findNewTerms(Vocabulary)}.
     *
     * @param input  The input to parse
     * @param source Original source of the input. Used for term occurrence generation
     */
    public abstract void parseContent(InputStream input, File source);

    /**
     * Gets the content which was previously parsed and processed by this instance.
     * <p>
     * This may return a different data that what was originally passed in {@link #parseContent(InputStream, File)}, as
     * the processing might have augmented the content, e.g., when new terms were processed.
     *
     * @return {@code InputStream} with processed content
     */
    public abstract InputStream getContent();

    /**
     * Finds term occurrences in the input stream.
     * <p>
     * {@link #parseContent(InputStream, File)} has to be called prior to this method.
     *
     * @return List of term occurrences identified in the input
     * @see #parseContent(InputStream, File)
     */
    public abstract List<TermOccurrence> findTermOccurrences();

    /**
     * Discovers new terms in the loaded file.
     * <p>
     * The specified vocabulary will be used mainly for term identifier resolution.
     * <p>
     * {@link #parseContent(InputStream, File)} has to be called prior to this method.
     *
     * @param vocabulary Vocabulary to which the new terms will be added
     * @return Newly discovered terms
     * @see #parseContent(InputStream, File)
     */
    public abstract List<Term> findNewTerms(Vocabulary vocabulary);

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
