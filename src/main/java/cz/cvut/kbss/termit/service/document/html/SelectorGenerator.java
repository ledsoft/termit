package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import org.jsoup.nodes.Element;

/**
 * Generator of HTML/XML selectors.
 */
public interface SelectorGenerator {

    /**
     * Generates selector for the specified elements' content.
     * <p>
     * The reason multiple elements are supported is because in case there are overlapping annotations, they are
     * represented by multiple elements using the <a href="https://en.wikipedia.org/wiki/Overlapping_markup#Joins">JOINS</a>
     * strategy.
     *
     * @param elements Elements to generate selector for. At least one must be provided
     * @return Selector for the text content of the specified elements
     */
    TermSelector generateSelector(Element... elements);
}
