package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Combines generators of selectors for HTML/XML elements.
 * <p>
 * Currently, {@link TextQuoteSelectorGenerator} and {@link TextPositionSelectorGenerator} are used.
 *
 * @see TextQuoteSelectorGenerator
 * @see TextPositionSelectorGenerator
 */
@Service
public class HtmlSelectorGenerators {

    private final List<SelectorGenerator> generators = Arrays
            .asList(new TextQuoteSelectorGenerator(), new TextPositionSelectorGenerator());

    /**
     * Generates selectors for the specified HTML/XML elements.
     *
     * @param elements Elements to generate selectors for
     * @return Set of generated selectors
     */
    public Set<TermSelector> generateSelectors(Element... elements) {
        return generators.stream().map(g -> g.generateSelector(elements)).collect(Collectors.toSet());
    }
}
