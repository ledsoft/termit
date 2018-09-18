package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.File;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TermOccurrenceDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao sut;

    @Test
    void findAllFindsOccurrencesOfTerm() {
        final Term term = new Term();
        term.setLabel("Test term");
        term.setUri(Generator.generateUri());
        final List<TermOccurrence> occurrences = generateOccurrences(term);

        final List<TermOccurrence> result = sut.findAll(term);
        assertEquals(occurrences.size(), result.size());
        for (TermOccurrence to : result) {
            assertTrue(occurrences.stream().anyMatch(o -> o.getUri().equals(to.getUri())));
        }
    }

    private List<TermOccurrence> generateOccurrences(Term term) {
        final File file = new File();
        file.setName("test");
        final List<TermOccurrence> occurrences = new ArrayList<>();
        final List<TermOccurrence> matching = new ArrayList<>();
        final Term other = new Term();
        other.setUri(Generator.generateUri());
        other.setLabel("Other term");
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermOccurrence to = new TermOccurrence();
            final Target target = new Target();
            target.setSource(file);
            final TextQuoteSelector selector = new TextQuoteSelector();
            selector.setExactMatch("test");
            selector.setPrefix("this is a ");
            selector.setSuffix(".");
            target.setSelectors(Collections.singleton(selector));
            to.setTargets(Collections.singleton(target));
            if (Generator.randomBoolean()) {
                to.setTerm(term);
                matching.add(to);
            } else {
                to.setTerm(other);
            }
            occurrences.add(to);
        }
        transactional(() -> {
            em.persist(file);
            em.persist(term);
            em.persist(other);
            occurrences.forEach(em::persist);
        });
        return matching;
    }
}