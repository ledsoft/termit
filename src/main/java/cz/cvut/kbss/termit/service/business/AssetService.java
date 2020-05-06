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

import cz.cvut.kbss.termit.dto.RecentlyModifiedAsset;
import cz.cvut.kbss.termit.service.repository.ResourceRepositoryService;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.service.repository.VocabularyRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssetService {

    private final ResourceRepositoryService resourceRepositoryService;

    private final TermRepositoryService termRepositoryService;

    private final VocabularyRepositoryService vocabularyRepositoryService;

    @Autowired
    public AssetService(ResourceRepositoryService resourceRepositoryService,
                        TermRepositoryService termRepositoryService,
                        VocabularyRepositoryService vocabularyRepositoryService) {
        this.resourceRepositoryService = resourceRepositoryService;
        this.termRepositoryService = termRepositoryService;
        this.vocabularyRepositoryService = vocabularyRepositoryService;
    }

    /**
     * Finds the specified number of most recently added/edited assets.
     *
     * @param limit Maximum number of assets to retrieve
     * @return List of recently added/edited assets
     */
    public List<RecentlyModifiedAsset> findLastEdited(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Maximum for recently edited assets must not be less than 0.");
        }
        final List<RecentlyModifiedAsset> resources = resourceRepositoryService.findLastEdited(limit);
        final List<RecentlyModifiedAsset> terms = termRepositoryService.findLastEdited(limit);
        final List<RecentlyModifiedAsset> vocabularies = vocabularyRepositoryService.findLastEdited(limit);
        final List<RecentlyModifiedAsset> result = mergeAssets(mergeAssets(resources, terms), vocabularies);
        return result.subList(0, Math.min(result.size(), limit));
    }

    private static List<RecentlyModifiedAsset> mergeAssets(List<RecentlyModifiedAsset> listOne,
                                                           List<RecentlyModifiedAsset> listTwo) {
        int oneIndex = 0;
        int twoIndex = 0;
        final List<RecentlyModifiedAsset> result = new ArrayList<>(listOne.size() + listTwo.size());
        while (oneIndex < listOne.size() && twoIndex < listTwo.size()) {
            if (listOne.get(oneIndex).getModified()
                       .compareTo(listTwo.get(twoIndex).getModified()) >= 0) {
                result.add(listOne.get(oneIndex));
                oneIndex++;
            } else {
                result.add(listTwo.get(twoIndex));
                twoIndex++;
            }
        }
        addRest(result, listOne, oneIndex);
        addRest(result, listTwo, twoIndex);
        return result;
    }

    private static void addRest(List<RecentlyModifiedAsset> target, List<RecentlyModifiedAsset> source, int index) {
        while (index < source.size()) {
            target.add(source.get(index++));
        }
    }
}
