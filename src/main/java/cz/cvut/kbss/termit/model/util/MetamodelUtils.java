package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.termit.exception.PersistenceException;

import java.lang.reflect.Field;
import java.util.Objects;

public class MetamodelUtils {

    private final EntityManagerFactory emf;

    public MetamodelUtils(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Gets field mapped by the specified attribute name in the specified entity class.
     * <p>
     * The attribute may reside in the specified entity class' ancestor.
     *
     * @param entityClass   Entity class whose attribute should be returned
     * @param attributeName Name of the attribute whose field should be returned
     * @return Field mapped by the specified attribute in the specified entity class
     */
    public Field getMappedField(Class<?> entityClass, String attributeName) {
        Objects.requireNonNull(entityClass);
        Objects.requireNonNull(attributeName);
        try {
            return emf.getMetamodel().entity(entityClass).getAttribute(attributeName).getJavaField();
        } catch (IllegalArgumentException e) {
            throw new PersistenceException("Unable to get mapped field.", e);
        }
    }
}
