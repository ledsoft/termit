package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.List;
import java.util.Optional;

/**
 * Generates a {@link TextQuoteSelector} for the specified elements' content.
 * <p>
 * Note that if there are multiple elements specified for generation, it is expected that no text node is present
 * between the elements in the actual page. Thus, the exact match is created by concatenating the text content of all
 * the elements.
 */
class TextQuoteSelectorGenerator implements SelectorGenerator {

    /**
     * Length of the generated prefix and suffix
     */
    static final int CONTEXT_LENGTH = 32;

    @Override
    public TextQuoteSelector generateSelector(Element... elements) {
        assert elements.length > 0;
        final TextQuoteSelector selector = new TextQuoteSelector(extractExactText(elements));
        extractPrefix(elements[0]).ifPresent(selector::setPrefix);
        extractSuffix(elements[elements.length - 1]).ifPresent(selector::setSuffix);
        return selector;
    }

    private Optional<String> extractPrefix(Element start) {
        Element current = start;
        Element previous = current;
        StringBuilder sb = new StringBuilder();
        while (current.hasParent()) {
            current = current.parent();
            final List<Node> previousSiblings = current.childNodes().subList(0, previous.siblingIndex());
            sb = extractNodeText(previousSiblings).append(sb);
            if (sb.length() >= CONTEXT_LENGTH) {
                break;
            }
            previous = current;
        }
        return sb.length() > 0 ? Optional.of(sb.substring(Math.max(0, sb.length() - CONTEXT_LENGTH))) :
               Optional.empty();
    }

    private Optional<String> extractSuffix(Element end) {
        Element current = end;
        Element previous = current;
        StringBuilder sb = new StringBuilder();
        while (current.hasParent()) {
            current = current.parent();
            final List<Node> previousSiblings = current.childNodes()
                                                       .subList(previous.siblingIndex() + 1, current.childNodeSize());
            sb.append(extractNodeText(previousSiblings));
            if (sb.length() >= CONTEXT_LENGTH) {
                break;
            }
            previous = current;
        }
        return sb.length() > 0 ? Optional.of(sb.substring(0, Math.min(sb.length(), CONTEXT_LENGTH))) : Optional.empty();
    }
}
