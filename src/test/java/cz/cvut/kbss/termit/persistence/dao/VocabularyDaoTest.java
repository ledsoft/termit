package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VocabularyDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private VocabularyDao sut;

    @Test
    void findAllReturnsVocabulariesOrderedByName() {
        final User user = Generator.generateUserWithId();
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> {
            final Vocabulary vocab = Generator.generateVocabulary();
            vocab.setUri(Generator.generateUri());
            vocab.setAuthor(user);
            vocab.setDateCreated(new Date());
            return vocab;
        }).collect(Collectors.toList());
        transactional(() -> {
            em.persist(user);
            vocabularies.forEach(em::persist);
        });

        final List<Vocabulary> result = sut.findAll();
        vocabularies.sort(Comparator.comparing(Vocabulary::getName));
        for (int i = 0; i < vocabularies.size(); i++) {
            assertEquals(vocabularies.get(i).getUri(), result.get(i).getUri());
        }
    }
}