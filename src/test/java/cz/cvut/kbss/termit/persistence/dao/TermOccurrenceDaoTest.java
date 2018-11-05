package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.TermOccurrence;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermOccurrenceDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private TermOccurrenceDao sut;

    @Test
    void findAllFindsOccurrencesOfTerm() {
        final Map<Term, List<TermOccurrence>> map = generateOccurrences();
        final Term term = map.keySet().iterator().next();
        final List<TermOccurrence> occurrences = map.get(term);

        final List<TermOccurrence> result = sut.findAll(term);
        assertEquals(occurrences.size(), result.size());
        for (TermOccurrence to : result) {
            assertTrue(occurrences.stream().anyMatch(o -> o.getUri().equals(to.getUri())));
        }
    }

    private Map<Term, List<TermOccurrence>> generateOccurrences(File... files) {
        final File[] filesToProcess;
        if (files.length == 0) {
            final File file = new File();
            file.setFileName("test.html");
            filesToProcess = new File[]{file};
        } else {
            filesToProcess = files;
        }
        final Term tOne = new Term();
        tOne.setUri(Generator.generateUri());
        tOne.setLabel("Term one");
        final Term tTwo = new Term();
        tTwo.setUri(Generator.generateUri());
        tTwo.setLabel("Term two");
        final Map<Term, List<TermOccurrence>> map = new HashMap<>();
        map.put(tOne, new ArrayList<>());
        map.put(tTwo, new ArrayList<>());
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final TermOccurrence to = new TermOccurrence();
            final Target target = new Target();
            target.setSource(filesToProcess[Generator.randomInt(0, filesToProcess.length)]);
            final TextQuoteSelector selector = new TextQuoteSelector("test");
            selector.setPrefix("this is a ");
            selector.setSuffix(".");
            target.setSelectors(Collections.singleton(selector));
            to.setTargets(Collections.singleton(target));
            if (Generator.randomBoolean()) {
                to.setTerm(tOne);
                map.get(tOne).add(to);
            } else {
                to.setTerm(tTwo);
                map.get(tTwo).add(to);
            }
        }
        transactional(() -> {
            for (File f : filesToProcess) {
                em.persist(f);
            }
            map.forEach((t, list) -> {
                em.persist(t);
                list.forEach(em::persist);
            });
        });
        return map;
    }

    @Test
    void findAllInFileReturnsTermOccurrencesWithTargetFile() {
        final File fOne = new File();
        fOne.setFileName("fOne.html");
        final File fTwo = new File();
        fTwo.setFileName("fTwo.html");
        final Map<Term, List<TermOccurrence>> allOccurrences = generateOccurrences(fOne, fTwo);
        final List<TermOccurrence> matching = allOccurrences.values().stream().flatMap(l -> l.stream().filter(to -> to
                .getTargets().stream().anyMatch(t -> t.getSource().getUri().equals(fOne.getUri()))))
                                                            .collect(Collectors.toList());

        final List<TermOccurrence> result = sut.findAllInFile(fOne);
        assertEquals(matching.size(), result.size());
        for (TermOccurrence to : result) {
            assertTrue(matching.stream().anyMatch(p -> to.getUri().equals(p.getUri())));
        }
    }
}