package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.exception.AnnotationGenerationException;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
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
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resolves term occurrences from RDFa-annotated HTML document.
 * <p>
 * This class is not thread-safe and not re-entrant.
 */
@Service("html")
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class HtmlTermOccurrenceResolver extends TermOccurrenceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlTermOccurrenceResolver.class);

    private final HtmlSelectorGenerators selectorGenerators;

    private Map<String, String> prefixes;

    @Autowired
    HtmlTermOccurrenceResolver(TermRepositoryService termService, HtmlSelectorGenerators selectorGenerators) {
        super(termService);
        this.selectorGenerators = selectorGenerators;
    }

    @Override
    public List<TermOccurrence> findTermOccurrences(InputStream content, File source) {
        final Document document = parseDocument(content);
        this.prefixes = resolvePrefixes(document);
        final Map<String, List<Element>> annotations = mapRDFaTermOccurrenceAnnotations(document);
        final List<TermOccurrence> result = new ArrayList<>(annotations.size());
        for (List<Element> elements : annotations.values()) {
            LOG.trace("Processing RDFa annotated elements {}.", elements);
            final Optional<TermOccurrence> occurrence = resolveAnnotation(elements, source);
            occurrence.ifPresent(to -> {
                LOG.trace("Found term occurrence {}.", to, source);
                result.add(to);
            });
        }
        return result;
    }

    private static Document parseDocument(InputStream content) {
        try {
            return Jsoup.parse(content, StandardCharsets.UTF_8.name(), "");
        } catch (IOException e) {
            throw new AnnotationGenerationException("Unable to read RDFa document.", e);
        }
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

    private Map<String, List<Element>> mapRDFaTermOccurrenceAnnotations(Document document) {
        final Map<String, List<Element>> map = new LinkedHashMap<>();
        final Elements elements = document.getElementsByAttribute(Constants.RDFa.ABOUT);
        for (Element element : elements) {
            if (!isTermOccurrence(element)) {
                continue;
            }
            map.computeIfAbsent(element.attr(Constants.RDFa.ABOUT), key -> new ArrayList<>()).add(element);
        }
        return map;
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

    private boolean isTermOccurrence(Element rdfaElem) {
        final String typesString = rdfaElem.attr(Constants.RDFa.TYPE);
        final String[] types = typesString.split(" ");
        // Perhaps we should check also for correct property?
        for (String type : types) {
            final String fullType = fullIri(type);
            if (fullType.equals(cz.cvut.kbss.termit.util.Vocabulary.s_c_vyskyt_termu)) {
                return true;
            }
        }
        return false;
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

    @Override
    public boolean supports(File source) {
        return source.getName().endsWith("html") || source.getName().endsWith("htm");
    }
}
