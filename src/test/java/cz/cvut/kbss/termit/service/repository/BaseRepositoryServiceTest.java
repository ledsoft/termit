package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.persistence.dao.UserDao;
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
    private UserDao userDao;

    @Autowired
    private BaseRepositoryServiceImpl sut;

    @Configuration
    public static class Config {

        @Bean
        public BaseRepositoryServiceImpl baseRepositoryService(UserDao userDao) {
            return new BaseRepositoryServiceImpl(userDao);
        }
    }

    @Mock
    private UserDao userDaoMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void persistExecutesTransactionalPersist() {
        final User user = Generator.generateUserWithId();

        sut.persist(user);
        assertTrue(userDao.exists(user.getUri()));
    }

    @Test
    void persistExecutesPrePersistMethodBeforePersistOnDao() {
        final User user = Generator.generateUser();
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));

        sut.persist(user);
        final InOrder inOrder = Mockito.inOrder(sut, userDaoMock);
        inOrder.verify(sut).prePersist(user);
        inOrder.verify(userDaoMock).persist(user);
    }

    @Test
    void updateExecutesTransactionalUpdate() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userDao.persist(user));

        final String updatedLastName = "Married";
        user.setLastName(updatedLastName);
        sut.update(user);

        final Optional<User> result = userDao.find(user.getUri());
        assertAll(() -> assertTrue(result.isPresent()),
                () -> assertEquals(updatedLastName, result.get().getLastName())
        );
    }

    @Test
    void updateExecutesPreUpdateMethodBeforeUpdateOnDao() {
        final User user = Generator.generateUser();
        when(userDaoMock.update(any())).thenReturn(user);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));

        sut.update(user);
        final InOrder inOrder = Mockito.inOrder(sut, userDaoMock);
        inOrder.verify(sut).preUpdate(user);
        inOrder.verify(userDaoMock).update(user);
    }

    @Test
    void updateInvokesPostUpdateAfterUpdateOnDao() {
        final User user = Generator.generateUser();
        final User returned = Generator.generateUser();
        when(userDaoMock.update(any())).thenReturn(returned);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));

        final User result = sut.update(user);
        final InOrder inOrder = Mockito.inOrder(sut, userDaoMock);
        inOrder.verify(userDaoMock).update(user);
        inOrder.verify(sut).postUpdate(returned);
        assertEquals(returned, result);
    }

    @Test
    void removeExecutesTransactionalRemove() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userDao.persist(user));

        sut.remove(user);
        assertFalse(userDao.exists(user.getUri()));
    }

    @Test
    void removeByIdExecutesTransactionalRemove() {
        final User user = Generator.generateUserWithId();
        transactional(() -> userDao.persist(user));

        sut.remove(user.getUri());
        assertFalse(userDao.exists(user.getUri()));
    }

    @Test
    void findExecutesPostLoadAfterLoadingEntityFromDao() {
        final User user = Generator.generateUserWithId();
        when(userDaoMock.find(user.getUri())).thenReturn(Optional.of(user));
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));

        final Optional<User> result = sut.find(user.getUri());
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        final InOrder inOrder = Mockito.inOrder(sut, userDaoMock);
        inOrder.verify(userDaoMock).find(user.getUri());
        inOrder.verify(sut).postLoad(user);
    }

    @Test
    void findAllExecutesPostLoadForEachLoadedEntity() {
        final List<User> users = IntStream.range(0, 5).mapToObj(i -> Generator.generateUserWithId())
                                          .collect(Collectors.toList());
        when(userDaoMock.findAll()).thenReturn(users);
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));

        final List<User> result = sut.findAll();
        assertEquals(users, result);
        final InOrder inOrder = Mockito.inOrder(sut, userDaoMock);
        inOrder.verify(userDaoMock).findAll();
        users.forEach(u -> inOrder.verify(sut).postLoad(u));
    }

    @Test
    void existsInvokesDao() {
        final URI id = Generator.generateUri();
        final BaseRepositoryServiceImpl sut = spy(new BaseRepositoryServiceImpl(userDaoMock));
        assertFalse(sut.exists(id));
        verify(userDaoMock).exists(id);
    }
}