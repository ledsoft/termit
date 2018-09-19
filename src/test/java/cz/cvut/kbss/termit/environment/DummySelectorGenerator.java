package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.service.document.html.SelectorGenerator;
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
        final TextQuoteSelector selector = new TextQuoteSelector();
        selector.setExactMatch(elements[0].wholeText());
        return selector;
    }
}
