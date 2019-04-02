package cz.cvut.kbss.termit.persistence.dao;

import cz.cvut.kbss.jopa.exceptions.NoResultException;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.model.Target;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TargetDao extends BaseDao<Target> {

    @Autowired
    public TargetDao(EntityManager em) {
        super(Target.class, em);
    }

    /**
     * Finds a target for the resource as a whole (i.e. not targets for selectors are returned)
     */
    public Optional<Target> findByWholeResource(final Resource resource) {
        Objects.requireNonNull(resource);
        try {
            return Optional.ofNullable(em.createNativeQuery(
                    "SELECT ?x WHERE {" + "?x a ?type ." + "?x ?hasSource ?resource . "
                            + "FILTER NOT EXISTS {?x ?hasSelector ?selector} }", Target.class)
                                         .setParameter("type", typeUri).setParameter("hasSource",
                            URI.create(Vocabulary.s_p_ma_zdroj)).setParameter("hasSelector",
                            URI.create(Vocabulary.s_p_ma_selektor_termu))
                                         .setParameter("resource", resource.getUri())
                                         .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}