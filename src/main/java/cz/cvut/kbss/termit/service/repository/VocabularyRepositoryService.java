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
package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.exception.VocabularyImportException;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Model;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.persistence.dao.AssetDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.VocabularyService;
import cz.cvut.kbss.termit.service.business.WorkspaceService;
import cz.cvut.kbss.termit.service.importer.VocabularyImportService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Validator;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VocabularyRepositoryService extends BaseAssetRepositoryService<Vocabulary> implements VocabularyService {

    private final IdentifierResolver idResolver;

    private final VocabularyDao vocabularyDao;

    private final ChangeRecordService changeRecordService;

    private final VocabularyImportService importService;

    private final WorkspaceService workspaceService;

    @Autowired
    public VocabularyRepositoryService(VocabularyDao vocabularyDao, IdentifierResolver idResolver,
                                       ChangeRecordService changeRecordService, Validator validator,
                                       VocabularyImportService importService, WorkspaceService workspaceService) {
        super(validator);
        this.vocabularyDao = vocabularyDao;
        this.idResolver = idResolver;
        this.changeRecordService = changeRecordService;
        this.importService = importService;
        this.workspaceService = workspaceService;
    }

    @Override
    protected AssetDao<Vocabulary> getPrimaryDao() {
        return vocabularyDao;
    }

    @Override
    public List<Vocabulary> findAll() {
        final List<Vocabulary> loaded = vocabularyDao.findAll(workspaceService.getCurrentWorkspace());
        return loaded.stream().map(this::postLoad).collect(Collectors.toList());
    }

    @Override
    protected void prePersist(Vocabulary instance) {
        super.prePersist(instance);
        if (instance.getUri() == null) {
            instance.setUri(generateIdentifier(instance.getLabel()));
        }
        verifyIdentifierUnique(instance);
        if (instance.getGlossary() == null) {
            instance.setGlossary(new Glossary());
        }
        if (instance.getModel() == null) {
            instance.setModel(new Model());
        }
    }

    @Override
    protected void preUpdate(Vocabulary instance) {
        super.preUpdate(instance);
        verifyVocabularyImports(instance);
    }

    /**
     * Ensures that possible vocabulary import removals are not prevented by existing inter-vocabulary term
     * relationships (terms from the updated vocabulary having parents from vocabularies whose import has been
     * removed).
     */
    private void verifyVocabularyImports(Vocabulary update) {
        final Vocabulary original = findRequired(update.getUri());
        final Set<URI> removedImports = new HashSet<>(Utils.emptyIfNull(original.getImportedVocabularies()));
        removedImports.removeAll(Utils.emptyIfNull(update.getImportedVocabularies()));
        final Set<URI> invalid = removedImports.stream().filter(ri -> vocabularyDao
                .hasInterVocabularyTermRelationships(update.getUri(), ri)).collect(
                Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new VocabularyImportException("Cannot remove imports of vocabularies " + invalid +
                    ", there are still relationships between terms.",
                    "error.vocabulary.update.imports.danglingTermReferences");
        }
    }

    @Override
    public URI generateIdentifier(String label) {
        Objects.requireNonNull(label);
        return idResolver.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, label);
    }

    @Override
    public Collection<URI> getTransitivelyImportedVocabularies(Vocabulary entity) {
        return vocabularyDao.getTransitivelyImportedVocabularies(entity);
    }

    @Override
    public List<AbstractChangeRecord> getChanges(Vocabulary asset) {
        return changeRecordService.getChanges(asset);
    }

    @Override
    public Vocabulary importVocabulary(MultipartFile file) {
        Objects.requireNonNull(file);
        return importService.importVocabulary(file);
    }

    @Override
    public long getLastModified() {
        return vocabularyDao.getLastModified();
    }
}
