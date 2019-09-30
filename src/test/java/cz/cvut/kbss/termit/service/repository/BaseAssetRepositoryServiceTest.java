package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class BaseAssetRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManager em;

    @Autowired
    private BaseAssetRepositoryServiceImpl sut;

    @Configuration
    public static class Config {

        @Bean
        public BaseAssetRepositoryServiceImpl baseRepositoryAssetService(VocabularyDao vocabularyDao,
                                                                         Validator validator) {
            return new BaseAssetRepositoryServiceImpl(vocabularyDao, validator);
        }

        @Bean
        public LocalValidatorFactoryBean validatorFactoryBean() {
            return new LocalValidatorFactoryBean();
        }
    }

    @BeforeEach
    void setUp() {
        final User author = Generator.generateUserWithId();
        transactional(() -> em.persist(author));
        Environment.setCurrentUser(author);
    }

    @Test
    void findRecentlyEditedLoadsRecentlyEditedItems() {
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> Generator.generateVocabularyWithId())
                                                       .collect(Collectors.toList());
        transactional(() -> vocabularies.forEach(em::persist));
        transactional(() -> setCreated(vocabularies));
        em.getEntityManagerFactory().getCache().evictAll();

        final int count = 2;
        final List<Vocabulary> all = sut.findAll();
        all.sort(Comparator.comparing(Vocabulary::getLastModifiedOrCreated).reversed());
        final List<Vocabulary> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertEquals(all.subList(0, count), result);
    }

    private void setCreated(List<Vocabulary> vocabularies) {
        final Repository repo = em.unwrap(Repository.class);
        final ValueFactory vf = repo.getValueFactory();
        try (final RepositoryConnection con = repo.getConnection()) {
            con.begin();
            for (int i = 0; i < vocabularies.size(); i++) {
                final Vocabulary r = vocabularies.get(i);
                con.remove(vf.createIRI(r.getUri().toString()),
                        vf.createIRI(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_datum_a_cas_vytvoreni), null);
                con.add(vf.createIRI(r.getUri().toString()),
                        vf.createIRI(cz.cvut.kbss.termit.util.Vocabulary.s_p_ma_datum_a_cas_vytvoreni),
                        vf.createLiteral(new Date(System.currentTimeMillis() - i * 1000 * 60)));
            }
            con.commit();
        }
    }

    @Test
    void findRecentlyEditedInvokesPostLoadForEachLoadedItem() {
        final List<Vocabulary> vocabularies = IntStream.range(0, 5).mapToObj(i -> Generator.generateVocabularyWithId())
                                                       .collect(Collectors.toList());
        final VocabularyDao vocabularyDao = mock(VocabularyDao.class);
        when(vocabularyDao.findLastEdited(anyInt())).thenReturn(vocabularies);
        final BaseAssetRepositoryService<Vocabulary> localSut = spy(
                new BaseAssetRepositoryServiceImpl(vocabularyDao, mock(Validator.class)));
        final int count = 117;
        localSut.findLastEdited(count);
        verify(vocabularyDao).findLastEdited(count);
        vocabularies.forEach(v -> verify(localSut).postLoad(v));
    }
}