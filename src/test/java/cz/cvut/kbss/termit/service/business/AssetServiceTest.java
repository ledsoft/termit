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

import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.dto.RecentlyModifiedAsset;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AssetServiceTest {

    @Mock
    private ResourceRepositoryService resourceService;

    @Mock
    private TermRepositoryService termService;

    @Mock
    private VocabularyRepositoryService vocabularyService;

    @InjectMocks
    private AssetService sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findRecentlyEditedCombinesOutputOfAllAssetServices() {
        final List<RecentlyModifiedAsset> allExpected = generateRecentlyModifiedAssets(15);

        final int count = allExpected.size();
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(allExpected.containsAll(result));
        verify(resourceService).findLastEdited(count);
        verify(termService).findLastEdited(count);
        verify(vocabularyService).findLastEdited(count);
    }

    private List<RecentlyModifiedAsset> generateRecentlyModifiedAssets(int count) {
        final List<RecentlyModifiedAsset> assets = new ArrayList<>();
        final List<RecentlyModifiedAsset> resources = new ArrayList<>();
        final List<RecentlyModifiedAsset> terms = new ArrayList<>();
        final List<RecentlyModifiedAsset> vocabularies = new ArrayList<>();
        final User author = Generator.generateUserWithId();
        for (int i = 0; i < count; i++) {
            Asset asset;
            RecentlyModifiedAsset rma = null;
            switch (i % 3) {
                case 0:
                    asset = Generator.generateResourceWithId();
                    rma = new RecentlyModifiedAsset(asset.getUri(), asset.getLabel(), new Date(), author.getUri(),
                            cz.cvut.kbss.termit.util.Vocabulary.s_c_resource);
                    resources.add(rma);
                    break;
                case 1:
                    asset = Generator.generateTermWithId();
                    rma = new RecentlyModifiedAsset(asset.getUri(), asset.getLabel(), new Date(), author.getUri(),
                            SKOS.CONCEPT);
                    terms.add(rma);
                    break;
                case 2:
                    asset = Generator.generateVocabularyWithId();
                    rma = new RecentlyModifiedAsset(asset.getUri(), asset.getLabel(), new Date(), author.getUri(),
                            Vocabulary.s_c_slovnik);
                    vocabularies.add(rma);
                    break;
            }
            rma.setModified(new Date(System.currentTimeMillis() - i * 1000));
            rma.setEditor(author);
            assets.add(rma);
        }
        when(resourceService.findLastEdited(anyInt())).thenReturn(resources);
        when(termService.findLastEdited(anyInt())).thenReturn(terms);
        when(vocabularyService.findLastEdited(anyInt())).thenReturn(vocabularies);
        return assets;
    }

    @Test
    void findLastEditedReturnsAssetsSortedByDateCreatedDescending() {
        final List<RecentlyModifiedAsset> allExpected = generateRecentlyModifiedAssets(6);
        allExpected.sort(Comparator.comparing(RecentlyModifiedAsset::getModified).reversed());
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(10);
        assertEquals(allExpected, result);
    }

    @Test
    void findLastEditedReturnsSublistOfAssetsWhenCountIsLessThanTotalNumber() {
        final List<RecentlyModifiedAsset> allExpected = generateRecentlyModifiedAssets(10);
        allExpected.sort(Comparator.comparing(RecentlyModifiedAsset::getModified).reversed());
        final int count = 6;
        final List<RecentlyModifiedAsset> result = sut.findLastEdited(count);
        assertEquals(allExpected.subList(0, count), result);
    }

    @Test
    void findLastEditedThrowsIllegalArgumentForCountLessThanZero() {
        assertThrows(IllegalArgumentException.class, () -> sut.findLastEdited(-1));
        verify(resourceService, never()).findLastEdited(anyInt());
        verify(termService, never()).findLastEdited(anyInt());
        verify(vocabularyService, never()).findLastEdited(anyInt());
    }
}
