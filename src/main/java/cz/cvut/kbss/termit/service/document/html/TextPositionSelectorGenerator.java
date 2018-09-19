package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TextPositionSelector;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.List;

/**
 * Generates a {@link TextPositionSelector} for the specified elements.
 * <p>
 * If there are multiple elements, the start position represent the character just before the first element's text
 * content and the end position is just after the last element's text content. It is assumed that there is no text
 * content in between the specified elements, so the end position is basically start position plus the length of text
 * content of the elements.
 * <p>
 * In order to be compatible with {@link cz.cvut.kbss.termit.model.selector.TextQuoteSelector} (as is required by the
 * specification), the generator uses only text content of the document, so any HTML/XML or other markup is ignored.
 */
class TextPositionSelectorGenerator extends SelectorGenerator {

    @Override
    public TextPositionSelector generateSelector(Element... elements) {
        assert elements.length > 0;
        final String textContent = extractExactText(elements);
        final TextPositionSelector selector = new TextPositionSelector();
        selector.setStart(resolveStartPosition(elements[0]));
        selector.setEnd(selector.getStart() + textContent.length());
        return selector;
    }

    private int resolveStartPosition(Element element) {
        final Elements ancestors = element.parents();
        Element previous = element;
        int counter = 0;
        for (Element parent : ancestors) {
            final List<Node> previousSiblings = parent.childNodes().subList(0, previous.siblingIndex());
            counter += extractNodeText(previousSiblings).length();
            previous = parent;
        }
        return counter;
    }
}
