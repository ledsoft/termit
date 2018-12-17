package cz.cvut.kbss.termit.persistence.dao.lucene;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static cz.cvut.kbss.termit.persistence.dao.lucene.LuceneSearchDao.LUCENE_WILDCARD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LuceneSearchDaoTest {

    @Mock
    private EntityManager emMock;

    @Mock
    private Query queryMock;

    private LuceneSearchDao sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(emMock.createNativeQuery(any(), anyString())).thenReturn(queryMock);
        when(queryMock.setParameter(anyString(), any())).thenReturn(queryMock);
        when(queryMock.setParameter(anyString(), any(), any())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());
        this.sut = new LuceneSearchDao(emMock);
    }

    @Test
    void fullTextSearchUsesOneTokenSearchStringAsDisjunctionOfExactAndWildcardMatch() {
        final String searchString = "test";
        sut.fullTextSearch(searchString);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(queryMock, atLeastOnce()).setParameter(anyString(), captor.capture(), any());
        final Optional<String> argument = captor.getAllValues().stream().filter(s -> s.startsWith(searchString))
                                                .findAny();
        assertTrue(argument.isPresent());
        assertEquals(searchString + " " + searchString + LUCENE_WILDCARD, argument.get());
    }

    @Test
    void fullTextSearchUsesLastTokenInMultiTokenSearchStringAsDisjunctionOfExactAndWildcardMatch() {
        final String lastToken = "token";
        final String searchString = "termOne termTwo " + lastToken;
        sut.fullTextSearch(searchString);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(queryMock, atLeastOnce()).setParameter(anyString(), captor.capture(), any());
        final Optional<String> argument = captor.getAllValues().stream().filter(s -> s.startsWith(searchString))
                                                .findAny();
        assertTrue(argument.isPresent());
        assertThat(argument.get(), containsString(lastToken + " " + lastToken + LUCENE_WILDCARD));
    }

    @Test
    void fullTextSearchDoesNotAddWildcardIfLastTokenAlreadyEndsWithWildcard() {
        final String searchString = "test token*";
        sut.fullTextSearch(searchString);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(queryMock, atLeastOnce()).setParameter(anyString(), captor.capture(), any());
        final Optional<String> argument = captor.getAllValues().stream().filter(s -> s.startsWith(searchString))
                                                .findAny();
        assertTrue(argument.isPresent());
        assertEquals(searchString, argument.get());
    }
}