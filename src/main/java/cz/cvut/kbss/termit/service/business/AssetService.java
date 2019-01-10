package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.resource.Resource;
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
    public List<Asset> findLastEdited(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Maximum for recently edited assets must not be less than 0.");
        }
        final List<Resource> resources = resourceRepositoryService.findLastEdited(limit);
        final List<Term> terms = termRepositoryService.findLastEdited(limit);
        final List<Vocabulary> vocabularies = vocabularyRepositoryService.findLastEdited(limit);
        final List<Asset> result = mergeAssets(mergeAssets(resources, terms), vocabularies);
        return result.subList(0, result.size() > limit ? limit : result.size());
    }

    private static List<Asset> mergeAssets(List<? extends Asset> listOne, List<? extends Asset> listTwo) {
        int oneIndex = 0;
        int twoIndex = 0;
        final List<Asset> result = new ArrayList<>(listOne.size() + listTwo.size());
        while (oneIndex < listOne.size() && twoIndex < listTwo.size()) {
            // TODO Add support for last edit when it is implemented
            if (listOne.get(oneIndex).getDateCreated().compareTo(listTwo.get(twoIndex).getDateCreated()) >= 0) {
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

    private static void addRest(List<Asset> target, List<? extends Asset> source, int index) {
        while (index < source.size()) {
            target.add(source.get(index++));
        }
    }
}
