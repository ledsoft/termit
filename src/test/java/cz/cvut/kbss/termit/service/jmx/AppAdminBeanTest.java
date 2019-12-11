package cz.cvut.kbss.termit.service.jmx;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.event.RefreshLastModifiedEvent;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class AppAdminBeanTest extends BaseServiceTestRunner {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private EntityManager em;

    @Mock
    private ApplicationEventPublisher eventPublisherMock;

    private AppAdminBean sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.sut = new AppAdminBean(eventPublisherMock, emf);
    }

    @Test
    void invalidateCachesClearsPersistenceSecondLevelCache() {
        final User entity = Generator.generateUserWithId();
        transactional(() -> em.persist(entity));
        assertTrue(emf.getCache().contains(User.class, entity.getUri(), new EntityDescriptor()));
        sut.invalidateCaches();
        assertFalse(emf.getCache().contains(User.class, entity.getUri(), new EntityDescriptor()));
    }

    @Test
    void invalidateCachesPublishesRefreshLastModifiedEvent() {
        sut.invalidateCaches();
        final ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisherMock).publishEvent(captor.capture());
        assertThat(captor.getValue(), instanceOf(RefreshLastModifiedEvent.class));
    }
}
