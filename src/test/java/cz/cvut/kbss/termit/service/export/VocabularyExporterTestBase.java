package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

abstract class VocabularyExporterTestBase extends BaseServiceTestRunner {

    @Autowired
    EntityManager em;

    Vocabulary vocabulary;

    void setUp() {
        this.vocabulary = Generator.generateVocabulary();
        final User author = Generator.generateUserWithId();
        vocabulary.setAuthor(author);
        vocabulary.setDateCreated(new Date());
        vocabulary.setUri(Generator.generateUri());
        transactional(() -> {
            em.persist(author);
            em.persist(vocabulary);
        });
    }

    List<Term> generateTerms() {
        final List<Term> terms = new ArrayList<>(10);
        for (int i = 0; i < Generator.randomInt(5, 10); i++) {
            final Term term = Generator.generateTermWithId();
            if (Generator.randomBoolean()) {
                term.setSources(Collections.singleton("PSP/c-1/p-2/b-c"));
            }
            terms.add(term);
        }
        vocabulary.getGlossary().setTerms(new HashSet<>(terms));
        transactional(() -> em.merge(vocabulary));
        return terms;
    }
}
