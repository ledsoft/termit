package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.UserAccountDao;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("service")
class BaseRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private UserAccountDao userAccountDao;

    @Autowired
    private Validator validator;

    @Autowired
    private BaseRepositoryServiceImpl sut;

    @Configuration
    public static class Config {

        @Bean
        public BaseRepositoryServiceImpl baseRepositoryService(UserAccountDao userAccountDao, Validator validator) {
            return new BaseRepositoryServiceImpl(userAccountDao, validator);
        }

        @Bean
        public LocalValidatorFactoryBean validatorFactoryBean() {
            return new LocalValidatorFactoryBean();
        }
    }

    @Mock
    private UserAccountDao userAccountDaoMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void persistExecutesTransactionalPersist() {
        final User user = Generator.generateUserWithId();

        sut.persist(user);
        assertTrue(userAccountDao.exists(user.getUri()));
    }

    @Test
    void persistExecutesPrePersistMethodBeforePersistOnDao() {
        final User user = Generator.generateUser();
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        sut.persist(user);
        final InOrder inOrder = Mockito.inOrder(sut, userAccountDaoMock);
        inOrder.verify(sut).prePersist(user);
        inOrder.verify(userAccountDaoMock).persist(user);
    }

    @Test
    void updateExecutesTransactionalUpdate() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userAccountDao.persist(user));

        final String updatedLastName = "Married";
        user.setLastName(updatedLastName);
        sut.update(user);

        final Optional<User> result = userAccountDao.find(user.getUri());
        assertAll(() -> assertTrue(result.isPresent()),
                () -> assertEquals(updatedLastName, result.get().getLastName())
        );
    }

    @Test
    void updateExecutesPreUpdateMethodBeforeUpdateOnDao() {
        final User user = Generator.generateUser();
        when(userAccountDaoMock.update(any())).thenReturn(user);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        sut.update(user);
        final InOrder inOrder = Mockito.inOrder(sut, userAccountDaoMock);
        inOrder.verify(sut).preUpdate(user);
        inOrder.verify(userAccountDaoMock).update(user);
    }

    @Test
    void updateInvokesPostUpdateAfterUpdateOnDao() {
        final User user = Generator.generateUser();
        final User returned = Generator.generateUser();
        when(userAccountDaoMock.update(any())).thenReturn(returned);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        final User result = sut.update(user);
        final InOrder inOrder = Mockito.inOrder(sut, userAccountDaoMock);
        inOrder.verify(userAccountDaoMock).update(user);
        inOrder.verify(sut).postUpdate(returned);
        assertEquals(returned, result);
    }

    @Test
    void removeExecutesTransactionalRemove() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userAccountDao.persist(user));

        sut.remove(user);
        assertFalse(userAccountDao.exists(user.getUri()));
    }

    @Test
    void removeByIdExecutesTransactionalRemove() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userAccountDao.persist(user));

        sut.remove(user.getUri());
        assertFalse(userAccountDao.exists(user.getUri()));
    }

    @Test
    void findExecutesPostLoadAfterLoadingEntityFromDao() {
        final User user = Generator.generateUserWithId();
        when(userAccountDaoMock.find(user.getUri())).thenReturn(Optional.of(user));
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        final Optional<User> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        final InOrder inOrder = Mockito.inOrder(sut, userAccountDaoMock);
        inOrder.verify(userAccountDaoMock).find(user.getUri());
        inOrder.verify(sut).postLoad(user);
    }

    @Test
    void findDoesNotExecutePostLoadWhenNoEntityIsFoundByDao() {
        when(userAccountDaoMock.find(any())).thenReturn(Optional.empty());
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        final Optional<User> result = sut.find(Generator.generateUri());
        assertFalse(result.isPresent());
        verify(sut, never()).postLoad(any());
    }

    @Test
    void findAllExecutesPostLoadForEachLoadedEntity() {
        final List<User> users = IntStream.range(0, 5).mapToObj(i -> Generator.generateUserWithId())
                                          .collect(Collectors.toList());
        when(userAccountDaoMock.findAll()).thenReturn(users);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        final List<User> result = sut.findAll();
        assertEquals(users, result);
        final InOrder inOrder = Mockito.inOrder(sut, userAccountDaoMock);
        inOrder.verify(userAccountDaoMock).findAll();
        users.forEach(u -> inOrder.verify(sut).postLoad(u));
    }

    @Test
    void existsInvokesDao() {
        final URI id = Generator.generateUri();
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));
        assertFalse(sut.exists(id));
        verify(userAccountDaoMock).exists(id);
    }

    @Test
    void removeInvokesPreAndPostHooks() {
        final UserAccount user = generateAccount();
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userAccountDaoMock, validator));

        final InOrder inOrder = inOrder(sut, userAccountDaoMock);
        sut.remove(user);
        inOrder.verify(sut).preRemove(user);
        inOrder.verify(userAccountDaoMock).remove(user);
        inOrder.verify(sut).postRemove(user);
    }
}