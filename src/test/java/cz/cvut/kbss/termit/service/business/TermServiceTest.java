package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.VocabularyExporters;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.CsvUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TermServiceTest {

    @Mock
    private VocabularyExporters exporters;

    @Mock
    private VocabularyService vocabularyService;

    @Mock
    private TermRepositoryService termRepositoryService;

    @InjectMocks
    private TermService sut;

    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(Generator.generateUri());
    }

    @Test
    void exportGlossaryGetsGlossaryExportForSpecifiedVocabularyFromExporters() {
        sut.exportGlossary(vocabulary, CsvUtils.MEDIA_TYPE);
        verify(exporters).exportVocabularyGlossary(vocabulary, CsvUtils.MEDIA_TYPE);
    }

    @Test
    void findVocabularyLoadsVocabularyFromRepositoryService() {
        when(vocabularyService.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        sut.findVocabulary(vocabulary.getUri());
        verify(vocabularyService).find(vocabulary.getUri());
    }

    @Test
    void findVocabularyThrowsNotFoundExceptionWhenVocabularyIsNotFound() {
        when(vocabularyService.find(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sut.findVocabulary(vocabulary.getUri()));
    }

    @Test
    void findFindsTermByIdInRepositoryService() {
        final URI uri = Generator.generateUri();
        sut.find(uri);
        verify(termRepositoryService).find(uri);
    }

    @Test
    void findAllRootsWithPagingRetrievesRootTermsFromVocabularyUsingRepositoryService() {
        sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        verify(termRepositoryService).findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC);
    }

    @Test
    void findAllRootsWithSearchStringRetrievesRootTermsFromVocabularyUsingRepositoryService() {
        final String searchString = "test";
        sut.findAllRoots(vocabulary, searchString);
        verify(termRepositoryService).findAllRoots(vocabulary, searchString);
    }

    @Test
    void getAssignmentsRetrievesTermAssignmentsFromRepositoryService() {
        final Term term = Generator.generateTermWithId();
        sut.getAssignments(term);
        verify(termRepositoryService).getAssignments(term);
    }

    @Test
    void persistUsesRepositoryServiceToPersistTermIntoVocabulary() {
        final Term term = Generator.generateTermWithId();
        sut.persistRoot(term, vocabulary);
        verify(termRepositoryService).addTermToVocabulary(term, vocabulary);
    }

    @Test
    void persistUsesRepositoryServiceToPersistTermAsChildOfSpecifiedParentTerm() {
        final Term parent = Generator.generateTermWithId();
        final Term toPersist = Generator.generateTermWithId();
        sut.persistChild(toPersist, parent);
        verify(termRepositoryService).addChildTerm(toPersist, parent);
    }

    @Test
    void updateUsesRepositoryServiceToUpdateTerm() {
        final Term term = Generator.generateTermWithId();
        sut.update(term);
        verify(termRepositoryService).update(term);
    }

    @Test
    void findSubTermsReturnsEmptyCollectionForTermWithoutSubTerms() {
        final Term term = Generator.generateTermWithId();
        final List<Term> result = sut.findSubTerms(term);
        assertTrue(result.isEmpty());
    }

    @Test
    void findSubTermsLoadsChildTermsOfTermUsingRepositoryService() {
        final Term parent = Generator.generateTermWithId();
        final List<Term> children = IntStream.range(0, 5).mapToObj(i -> {
            final Term child = Generator.generateTermWithId();
            when(termRepositoryService.find(child.getUri())).thenReturn(Optional.of(child));
            return child;
        }).collect(Collectors.toList());
        parent.setSubTerms(children.stream().map(Term::getUri).collect(Collectors.toSet()));

        final List<Term> result = sut.findSubTerms(parent);
        assertEquals(children.size(), result.size());
        assertTrue(children.containsAll(result));
    }

    @Test
    void existsInVocabularyChecksForLabelExistenceInVocabularyViaRepositoryService() {
        final String label = "test";
        sut.existsInVocabulary(label, vocabulary);
        verify(termRepositoryService).existsInVocabulary(label, vocabulary);
    }
}