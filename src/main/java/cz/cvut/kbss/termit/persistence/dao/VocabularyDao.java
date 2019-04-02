/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.DescriptorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class VocabularyDao extends AssetDao<Vocabulary> {

    @Autowired
    public VocabularyDao(EntityManager em) {
        super(Vocabulary.class, em);
    }

    @Override
    public List<Vocabulary> findAll() {
        final List<Vocabulary> result = super.findAll();
        result.sort(Comparator.comparing(Vocabulary::getLabel));
        return result;
    }

    @Override
    public Optional<Vocabulary> find(URI id) {
        Objects.requireNonNull(id);
        try {
            return Optional.ofNullable(em.find(type, id, DescriptorFactory.vocabularyDescriptor(id)));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void persist(Vocabulary entity) {
        Objects.requireNonNull(entity);
        try {
            em.persist(entity, DescriptorFactory.vocabularyDescriptor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Vocabulary update(Vocabulary entity) {
        Objects.requireNonNull(entity);
        try {
            // Evict possibly cached instance loaded from default context
            em.getEntityManagerFactory().getCache().evict(Vocabulary.class, entity.getUri(), null);
            return em.merge(entity, DescriptorFactory.vocabularyDescriptor(entity));
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Updates glossary contained in the specified vocabulary.
     * <p>
     * The vocabulary is passed for correct context resolution, as glossary existentially depends on its owning
     * vocabulary.
     *
     * @param entity Owner of the updated glossary
     * @return The updated entity
     */
    public Glossary updateGlossary(Vocabulary entity) {
        Objects.requireNonNull(entity);
        return em.merge(entity.getGlossary(), DescriptorFactory.glossaryDescriptor(entity));
    }
}
