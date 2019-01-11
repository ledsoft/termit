package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
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
        transactional(() -> {
            for (int i = 0; i < vocabularies.size(); i++) {
                vocabularies.get(i).setCreated(new Date(System.currentTimeMillis() - i * 1000));
                em.merge(vocabularies.get(i));
            }
        });

        final int count = 2;
        final List<Vocabulary> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        vocabularies.sort(Comparator.comparing(Vocabulary::getCreated).reversed());
        assertEquals(vocabularies.subList(0, count), result);
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