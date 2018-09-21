package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Document;
import cz.cvut.kbss.termit.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentDaoTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private DocumentDao sut;

    @Test
    void findAllReturnsDocumentsOrderedByNameAscending() {
        final User author = Generator.generateUserWithId();
        final List<Document> documents = IntStream.range(0, 10).mapToObj(i -> {
            final Document doc = new Document();
            doc.setName("Document-" + i);
            doc.setUri(Generator.generateUri());
            doc.setAuthor(author);
            doc.setDateCreated(new Date());
            return doc;
        }).collect(Collectors.toList());
        Collections.shuffle(documents);
        transactional(() -> {
            em.persist(author);
            documents.forEach(em::persist);
        });

        documents.sort(Comparator.comparing(Document::getName));
        final List<Document> result = sut.findAll();
        assertEquals(documents, result);
    }
}