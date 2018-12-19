package cz.cvut.kbss.termit.service.export;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

abstract class VocabularyExporterTestBase extends BaseServiceTestRunner {

    @Autowired
    EntityManager em;

    Vocabulary vocabulary;

    void setUp() {
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
        final User author = Generator.generateUserWithId();
        Environment.setCurrentUser(author);
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
        transactional(() -> {
            em.merge(vocabulary.getGlossary());
            terms.forEach(em::persist);
            // Simulating inferred inverse property je_pojmem_ze_slovniku
            final Repository repo = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repo.getConnection()) {
                final ValueFactory vf = repo.getValueFactory();
                final IRI vocabIri = vf.createIRI(vocabulary.getUri().toString());
                final IRI inVocab = vf.createIRI(cz.cvut.kbss.termit.util.Vocabulary.s_p_je_pojmem_ze_slovniku);
                conn.begin();
                for (Term t : terms) {
                    conn.add(vf.createIRI(t.getUri().toString()), inVocab, vocabIri);
                }
                conn.commit();
            }
        });
        return terms;
    }
}
