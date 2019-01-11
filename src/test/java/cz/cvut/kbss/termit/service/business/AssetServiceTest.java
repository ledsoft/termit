package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
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
        final List<Asset> assets = generateAssets(15);

        final int count = assets.size();
        final List<Asset> result = sut.findLastEdited(count);
        assertEquals(count, result.size());
        assertTrue(assets.containsAll(result));
        verify(resourceService).findLastEdited(count);
        verify(termService).findLastEdited(count);
        verify(vocabularyService).findLastEdited(count);
    }

    private List<Asset> generateAssets(int count) {
        final List<Asset> assets = new ArrayList<>();
        final List<Resource> resources = new ArrayList<>();
        final List<Term> terms = new ArrayList<>();
        final List<Vocabulary> vocabularies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Asset asset = null;
            switch (i % 3) {
                case 0:
                    asset = Generator.generateResourceWithId();
                    resources.add((Resource) asset);
                    break;
                case 1:
                    asset = Generator.generateTermWithId();
                    terms.add((Term) asset);
                    break;
                case 2:
                    asset = Generator.generateVocabularyWithId();
                    vocabularies.add((Vocabulary) asset);
                    break;
            }
            asset.setCreated(new Date(System.currentTimeMillis() - i * 1000));
            assets.add(asset);
        }
        when(resourceService.findLastEdited(anyInt())).thenReturn(resources);
        when(termService.findLastEdited(anyInt())).thenReturn(terms);
        when(vocabularyService.findLastEdited(anyInt())).thenReturn(vocabularies);
        return assets;
    }

    @Test
    void findRecentlyEditedReturnsAssetsSortedByDateCreatedDescending() {
        final List<Asset> assets = generateAssets(6);
        assets.sort(Comparator.comparing(Asset::getCreated).reversed());
        final List<Asset> result = sut.findLastEdited(10);
        assertEquals(assets, result);
    }

    @Test
    void findRecentlyEditedReturnsSublistOfAssetsWhenCountIsLessThanTotalNumber() {
        final List<Asset> assets = generateAssets(10);
        assets.sort(Comparator.comparing(Asset::getCreated).reversed());
        final int count = 6;
        final List<Asset> result = sut.findLastEdited(count);
        assertEquals(assets.subList(0, count), result);
    }

    @Test
    void findRecentlyEditedThrowsIllegalArgumentForCountLessThanZero() {
        assertThrows(IllegalArgumentException.class, () -> sut.findLastEdited(-1));
        verify(resourceService, never()).findLastEdited(anyInt());
        verify(termService, never()).findLastEdited(anyInt());
        verify(vocabularyService, never()).findLastEdited(anyInt());
    }
}