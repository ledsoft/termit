package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.*;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.document.TermOccurrenceResolver;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves term occurrences from RDFa-annotated HTML document.
 * <p>
 * This class is not thread-safe and not re-entrant.
 */
@Service("html")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HtmlTermOccurrenceResolver extends TermOccurrenceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlTermOccurrenceResolver.class);

    private final HtmlSelectorGenerators selectorGenerators;

    private final IdentifierResolver idResolver;

    private Document document;
    private File source;

    private Map<String, String> prefixes;

    private Map<String, List<Element>> annotatedElements;

    @Autowired
    HtmlTermOccurrenceResolver(TermRepositoryService termService, HtmlSelectorGenerators selectorGenerators,
                               IdentifierResolver idResolver) {
        super(termService);
        this.selectorGenerators = selectorGenerators;
        this.idResolver = idResolver;
    }

    @Override
    public void parseContent(InputStream input, File source) {
        try {
            this.source = source;
            this.document = Jsoup.parse(input, StandardCharsets.UTF_8.name(),
                    source.getOrigin() != null ? source.getOrigin().toString() : "");
            this.prefixes = resolvePrefixes(document);
        } catch (IOException e) {
            throw new AnnotationGenerationException("Unable to read RDFa document.", e);
        }
    }

    @Override
    public InputStream getContent() {
        assert document != null;
        return new ByteArrayInputStream(document.toString().getBytes());
    }

    private static Map<String, String> resolvePrefixes(Document document) {
        final Map<String, String> map = new HashMap<>(4);
        final Elements prefixElements = document.getElementsByAttribute(Constants.RDFa.PREFIX);
        prefixElements.forEach(element -> {
            final String prefixStr = element.attr(Constants.RDFa.PREFIX);
            final String[] prefixDefinitions = prefixStr.split("[^:] ");
            for (String def : prefixDefinitions) {
                final String[] split = def.split(": ");
                assert split.length == 2;
                map.put(split[0].trim(), split[1].trim());
            }
        });
        return map;
    }

    @Override
    public List<Term> findNewTerms(Vocabulary vocabulary) {
        assert document != null;
        mapRDFaTermOccurrenceAnnotations();
        final List<Term> newTerms = new ArrayList<>(annotatedElements.size());
        for (List<Element> annotation : annotatedElements.values()) {
            List<Element> parts = annotation.stream().filter(e -> !existingTerm(e)).collect(Collectors.toList());
            if (parts.isEmpty()) {
                continue;
            }
            final String label = parts.stream().map(elem -> {
                final String content = elem.attr(Constants.RDFa.CONTENT).trim();
                return content.isEmpty() ? elem.wholeText() : content;
            }).collect(Collectors.joining(" "));
            final Term newTerm = new Term();
            newTerm.setLabel(label);
            newTerm.setUri(idResolver.generateIdentifier(
                    idResolver.buildNamespace(vocabulary.getUri().toString(), Constants.TERM_NAMESPACE_SEPARATOR),
                    label));
            LOG.trace("Generated new term with URI '{}' for suggested label '{}'.", newTerm.getUri(), label);
            parts.forEach(elem -> elem.attr(Constants.RDFa.RESOURCE, newTerm.getUri().toString()));
            newTerms.add(newTerm);
        }
        return newTerms;
    }

    private void mapRDFaTermOccurrenceAnnotations() {
        this.annotatedElements = new LinkedHashMap<>();
        final Elements elements = document.getElementsByAttribute(Constants.RDFa.ABOUT);
        for (Element element : elements) {
            if (isNotTermOccurrence(element)) {
                continue;
            }
            annotatedElements.computeIfAbsent(element.attr(Constants.RDFa.ABOUT), key -> new ArrayList<>())
                             .add(element);
        }
    }

    private boolean isNotTermOccurrence(Element rdfaElem) {
        if (!rdfaElem.hasAttr(Constants.RDFa.RESOURCE) && !rdfaElem.hasAttr(Constants.RDFa.CONTENT)) {
            return true;
        }
        final String typesString = rdfaElem.attr(Constants.RDFa.TYPE);
        final String[] types = typesString.split(" ");
        // Perhaps we should check also for correct property?
        for (String type : types) {
            final String fullType = fullIri(type);
            if (fullType.equals(cz.cvut.kbss.termit.util.Vocabulary.s_c_vyskyt_termu)) {
                return false;
            }
        }
        return true;
    }

    private String fullIri(String possiblyPrefixed) {
        possiblyPrefixed = possiblyPrefixed.trim();
        final int colonIndex = possiblyPrefixed.indexOf(':');
        if (colonIndex == -1) {
            return possiblyPrefixed;
        }
        final String prefix = possiblyPrefixed.substring(0, colonIndex);
        if (!prefixes.containsKey(prefix)) {
            return possiblyPrefixed;
        }
        final String localName = possiblyPrefixed.substring(colonIndex + 1);
        return prefixes.get(prefix) + localName;
    }

    private static boolean existingTerm(Element rdfaElem) {
        return rdfaElem.hasAttr(Constants.RDFa.RESOURCE);
    }

    @Override
    public List<TermOccurrence> findTermOccurrences() {
        assert document != null;
        if (annotatedElements == null) {
            mapRDFaTermOccurrenceAnnotations();
        }
        final List<TermOccurrence> result = new ArrayList<>(annotatedElements.size());
        for (List<Element> elements : annotatedElements.values()) {
            LOG.trace("Processing RDFa annotated elements {}.", elements);
            final Optional<TermOccurrence> occurrence = resolveAnnotation(elements, source);
            occurrence.ifPresent(to -> {
                LOG.trace("Found term occurrence {}.", to, source);
                result.add(to);
            });
        }
        return result;
    }

    private Optional<TermOccurrence> resolveAnnotation(List<Element> rdfaElem, File source) {
        assert !rdfaElem.isEmpty();
        final String termId = fullIri(rdfaElem.get(0).attr(Constants.RDFa.RESOURCE));
        if (termId.isEmpty()) {
            LOG.warn("Missing term identifier in RDFa element {}. Skipping it.", rdfaElem);
            return Optional.empty();
        }
        final Term term = termService.find(URI.create(termId)).orElseThrow(() -> new AnnotationGenerationException(
                "Term with id " + termId + " denoted by RDFa element " + rdfaElem + " not found."));
        final TermOccurrence occurrence = createOccurrence(term);
        final Target target = new Target();
        target.setSource(source);
        target.setSelectors(selectorGenerators.generateSelectors(rdfaElem.toArray(new Element[0])));
        occurrence.addTarget(target);
        return Optional.of(occurrence);
    }

    @Override
    public boolean supports(File source) {
        return source.getFileName().endsWith("html") || source.getFileName().endsWith("htm");
    }
}
