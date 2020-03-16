/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.dto.TermInfo;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.export.VocabularyExporters;
import cz.cvut.kbss.termit.service.repository.ChangeRecordService;
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

    @Mock
    private ChangeRecordService changeRecordService;

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
        sut.findVocabularyRequired(vocabulary.getUri());
        verify(vocabularyService).find(vocabulary.getUri());
    }

    @Test
    void findVocabularyThrowsNotFoundExceptionWhenVocabularyIsNotFound() {
        when(vocabularyService.find(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sut.findVocabularyRequired(vocabulary.getUri()));
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
    void findAllBySearchStringRetrievesMatchingTermsFromVocabularyUsingRepositoryService() {
        final String searchString = "test";
        sut.findAll(searchString, vocabulary);
        verify(termRepositoryService).findAll(searchString, vocabulary);
    }

    @Test
    void getAssignmentInfoRetrievesTermAssignmentInfoFromRepositoryService() {
        final Term term = Generator.generateTermWithId();
        sut.getAssignmentInfo(term);
        verify(termRepositoryService).getAssignmentsInfo(term);
    }

    @Test
    void persistUsesRepositoryServiceToPersistTermIntoVocabulary() {
        final Term term = Generator.generateTermWithId();
        sut.persistRoot(term, vocabulary);
        verify(termRepositoryService).addRootTermToVocabulary(term, vocabulary);
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
        parent.setSubTerms(children.stream().map(TermInfo::new).collect(Collectors.toSet()));

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

    @Test
    void findAllRetrievesAllTermsFromVocabularyUsingRepositoryService() {
        sut.findAll(vocabulary);
        verify(termRepositoryService).findAll(vocabulary);
    }

    @Test
    void getReferenceRetrievesTermReferenceFromRepositoryService() {
        final URI iri = Generator.generateUri();
        sut.getReference(iri);
        verify(termRepositoryService).getReference(iri);
    }

    @Test
    void getRequiredReferenceRetrievesTermReferenceFromRepositoryService() {
        final URI iri = Generator.generateUri();
        sut.getRequiredReference(iri);
        verify(termRepositoryService).getRequiredReference(iri);
    }

    @Test
    void findAllRootsIncludingImportsRetrievesRootTermsUsingRepositoryService() {
        sut.findAllRootsIncludingImports(vocabulary, Constants.DEFAULT_PAGE_SPEC);
        verify(termRepositoryService).findAllRootsIncludingImported(vocabulary, Constants.DEFAULT_PAGE_SPEC);
    }

    @Test
    void findAllIncludingImportedBySearchStringRetrievesMatchingTermsUsingRepositoryService() {
        final String searchString = "test";
        sut.findAllIncludingImported(searchString, vocabulary);
        verify(termRepositoryService).findAllIncludingImported(searchString, vocabulary);
    }

    @Test
    void removeRemovesTermViaRepositoryService() {
        final Term toRemove = Generator.generateTermWithId();
        sut.remove(toRemove);
        verify(termRepositoryService).remove(toRemove);
    }

    @Test
    void getChangesRetrievesChangeRecordsFromChangeRecordService() {
        final Term asset = Generator.generateTermWithId();
        sut.getChanges(asset);
        verify(changeRecordService).getChanges(asset);
    }
}
