package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import org.jsoup.nodes.Element;

/**
 * Generator of HTML/XML selectors.
 */
public interface SelectorGenerator {

    /**
     * Generates selector for the specified element's content.
     *
     * @param element Element to generate selector for
     * @return Selector
     */
    TermSelector createSelector(Element element);
}
