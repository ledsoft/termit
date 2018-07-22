package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.util.EntityToOwlClassMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Base implementation of the generic DAO API.
 */
public abstract class BaseDao<T> implements GenericDao<T> {

    // TODO Error handling

    protected static final Logger LOG = LoggerFactory.getLogger(BaseDao.class);

    protected final Class<T> type;
    protected final URI typeUri;

    protected final EntityManager em;

    protected BaseDao(Class<T> type, EntityManager em) {
        this.type = type;
        this.typeUri = URI.create(EntityToOwlClassMapper.getOwlClassForEntity(type));
        this.em = em;
    }

    @Override
    public List<T> findAll() {
        return em.createNativeQuery("SELECT ?x WHERE { ?x a ?type . }", type).setParameter("type", typeUri)
                 .getResultList();
    }

    @Override
    public Optional<T> find(URI id) {
        Objects.requireNonNull(id);
        return Optional.ofNullable(em.find(type, id));
    }

    @Override
    public void persist(T entity) {
        Objects.requireNonNull(entity);
        em.persist(entity);
    }

    @Override
    public void persist(Collection<T> entities) {
        Objects.requireNonNull(entities);
        entities.forEach(em::persist);
    }

    @Override
    public T update(T entity) {
        Objects.requireNonNull(entity);
        return em.merge(entity);
    }

    @Override
    public void remove(T entity) {
        Objects.requireNonNull(entity);
        em.remove(em.merge(entity));
    }

    @Override
    public void remove(URI id) {
        Objects.requireNonNull(id);
        find(id).ifPresent(em::remove);
    }

    @Override
    public boolean exists(URI id) {
        Objects.requireNonNull(id);
        return em.createNativeQuery("ASK { ?x a ?type . }", Boolean.class).setParameter("x", id)
                 .setParameter("type", typeUri).getSingleResult();
    }
}
