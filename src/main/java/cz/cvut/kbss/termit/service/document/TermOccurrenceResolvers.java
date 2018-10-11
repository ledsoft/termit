package cz.cvut.kbss.termit.service.document;

import cz.cvut.kbss.termit.service.document.html.HtmlTermOccurrenceResolver;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

/**
 * Utility class providing instances of prototype-scoped {@link TermOccurrenceResolver} implementations.
 * <p>
 * Since the beans are prototype-scoped, each call to the methods will produce a new instance of the corresponding
 * class.
 */
@Component
abstract class TermOccurrenceResolvers {

    @Lookup
    abstract HtmlTermOccurrenceResolver htmlTermOccurrenceResolver();
}
