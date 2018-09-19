package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.persistence.dao.TermOccurrenceDao;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Creates annotations (term occurrences) for vocabulary terms.
 */
@Service
public class AnnotationGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationGenerator.class);

    private final TermRepositoryService termService;

    private final TermOccurrenceDao termOccurrenceDao;

    private final SelectorGenerator selectorGenerator;

    @Autowired
    public AnnotationGenerator(TermRepositoryService termService, TermOccurrenceDao termOccurrenceDao,
                               SelectorGenerator selectorGenerator) {
        this.termService = termService;
        this.termOccurrenceDao = termOccurrenceDao;
        this.selectorGenerator = selectorGenerator;
    }

    /**
     * Generates annotations (term occurrences) for terms identified in the specified document.
     * <p>
     * The document is annotated using RDFa to indicate term occurrences in the text.
     *
     * @param content    Content of file with identified term occurrences
     * @param source     Source file of the document
     * @param vocabulary Vocabulary whose terms occur in the document
     */
    @Transactional
    public void generateAnnotations(InputStream content, File source, Vocabulary vocabulary) {
        final Elements rdfaElements = retrieveRDFaElements(content);
        for (Element element : rdfaElements) {
            LOG.trace("Processing RDFa annotated element {}.", element);
            final TermOccurrence occurrence = resolveAnnotation(element, source);
            LOG.trace("Found term occurrence {} in file {}.", occurrence, source);
            termOccurrenceDao.persist(occurrence);
        }
    }

    private Elements retrieveRDFaElements(InputStream content) {
        try {
            final Document document = Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
            return document.getElementsByAttribute(Constants.RDFa.ABOUT);
        } catch (IOException e) {
            throw new AnnotationGenerationException("Unable to read RDFa document.", e);
        }
    }

    private TermOccurrence resolveAnnotation(Element rdfaElem, File source) {
        final String termUri = rdfaElem.attr(Constants.RDFa.RESOURCE);
        final Term term = termService.find(URI.create(termUri)).orElseThrow(() -> new NotFoundException(
                "Term with id " + termUri + " denoted by RDFa element " + rdfaElem + " not found."));
        final TermOccurrence occurrence = createOccurrence(term);
        final Target target = new Target();
        target.setSource(source);
        target.setSelectors(Collections.singleton(selectorGenerator.createSelector(rdfaElem)));
        occurrence.addTarget(target);
        return occurrence;
    }

    private TermOccurrence createOccurrence(Term term) {
        final TermOccurrence occurrence = new TermOccurrence();
        occurrence.setTerm(term);
        return occurrence;
    }
}
