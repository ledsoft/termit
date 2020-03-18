package cz.cvut.kbss.termit.persistence.dao.changetracking;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.Asset;
import cz.cvut.kbss.termit.model.changetracking.AbstractChangeRecord;
import cz.cvut.kbss.termit.model.changetracking.UpdateChangeRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class ChangeRecordDao {

    private final ChangeTrackingContextResolver contextResolver;

    private final EntityManager em;

    public ChangeRecordDao(ChangeTrackingContextResolver contextResolver, EntityManager em) {
        this.contextResolver = contextResolver;
        this.em = em;
    }

    /**
     * Persists the specified change record into the specified repository context.
     *
     * @param record       Record to save
     * @param changedAsset The changed asset
     */
    public void persist(AbstractChangeRecord record, Asset changedAsset) {
        Objects.requireNonNull(record);
        final EntityDescriptor descriptor = new EntityDescriptor(
                contextResolver.resolveChangeTrackingContext(changedAsset));
        descriptor.addAttributeDescriptor(AbstractChangeRecord.getAuthorField(), new EntityDescriptor());
        try {
            em.persist(record, descriptor);
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Finds all change records to the specified asset.
     *
     * @param asset The changed asset
     * @return List of change records ordered by timestamp (descending)
     */
    public List<AbstractChangeRecord> findAll(Asset asset) {
        Objects.requireNonNull(asset);
        try {
            final EntityType<AbstractChangeRecord> et = em.getMetamodel().entity(AbstractChangeRecord.class);
            final EntityType<UpdateChangeRecord> updateEt = em.getMetamodel().entity(UpdateChangeRecord.class);
            return em.createNativeQuery("SELECT ?r WHERE {" +
                    "?r a ?changeRecord ;" +
                    "?relatesTo ?asset ;" +
                    "?hasTime ?timestamp ." +
                    "OPTIONAL { ?r ?hasChangedAttribute ?attribute . }" +
                    "} ORDER BY DESC(?timestamp) ?attribute", AbstractChangeRecord.class)
                     .setParameter("changeRecord", et.getIRI().toURI())
                     .setParameter("relatesTo", et.getAttribute("changedEntity").getIRI().toURI())
                     .setParameter("hasChangedAttribute", updateEt.getAttribute("changedAttribute").getIRI().toURI())
                     .setParameter("hasTime", et.getAttribute("timestamp").getIRI().toURI())
                     .setParameter("asset", asset.getUri()).getResultList();
        } catch (RuntimeException e) {
            throw new PersistenceException(e);
        }
    }
}
