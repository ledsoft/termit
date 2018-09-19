package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.service.document.SelectorGenerator;
import org.jsoup.nodes.Element;

/**
 * Generates text quote selector without prefix and suffix.
 * <p>
 * For testing purposes only.
 */
public class DummySelectorGenerator implements SelectorGenerator {
    @Override
    public TermSelector createSelector(Element element) {
        final TextQuoteSelector selector = new TextQuoteSelector();
        selector.setExactMatch(element.wholeText());
        return selector;
    }
}
