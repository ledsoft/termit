package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("textQuote")
public class TextQuoteSelectorGenerator implements SelectorGenerator {

    /**
     * Length of the generated prefix and suffix
     */
    static final int CONTEXT_LENGTH = 32;

    @Override
    public TextQuoteSelector generateSelector(Element... elements) {
        assert elements.length > 0;
        final TextQuoteSelector selector = new TextQuoteSelector();
        selector.setExactMatch(extractExactText(elements));
        extractPrefix(elements[0]).ifPresent(selector::setPrefix);
        extractSuffix(elements[elements.length - 1]).ifPresent(selector::setSuffix);
        return selector;
    }

    private String extractExactText(Element[] elements) {
        final StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            sb.append(element.wholeText());
        }
        return sb.toString();
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

    private StringBuilder extractNodeText(Iterable<Node> nodes) {
        final StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            sb.append(node instanceof TextNode ? ((TextNode) node).getWholeText() : ((Element) node).wholeText());
        }
        return sb;
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
