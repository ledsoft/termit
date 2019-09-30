package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import org.jsoup.nodes.Element;

/**
 * Generates text quote selector without prefix and suffix.
 * <p>
 * For testing purposes only.
 */
public class DummySelectorGenerator implements SelectorGenerator {

    @Override
    public TermSelector generateSelector(Element... elements) {
        assert elements.length > 0;
        return new TextQuoteSelector(elements[0].wholeText());
    }
}
