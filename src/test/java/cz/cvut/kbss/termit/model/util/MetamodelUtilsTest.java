package cz.cvut.kbss.termit.model.util;

import cz.cvut.kbss.jopa.exceptions.OWLPersistenceException;
import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.termit.exception.PersistenceException;
import cz.cvut.kbss.termit.model.HasProvenanceData;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.persistence.dao.BaseDaoTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class MetamodelUtilsTest extends BaseDaoTestRunner {

    @Autowired
    private EntityManagerFactory emf;

    private MetamodelUtils sut;

    @BeforeEach
    void setUp() {
        this.sut = new MetamodelUtils(emf);
    }

    @Test
    void getMappedFieldReturnsFieldMappedBySpecifiedAttributeName() throws Exception {
        assertEquals(Term.class.getDeclaredField("label"), sut.getMappedField(Term.class, "label"));
    }

    @Test
    void getMappedFieldReturnsFieldMappedBySpecifiedAttributeNameInSuperclass() throws Exception {
        assertEquals(HasProvenanceData.class.getDeclaredField("author"), sut.getMappedField(File.class, "author"));
    }

    @Test
    void getMappedFieldThrowsPersistenceExceptionWhenAttributeDoesNotExistInEntity() {
        assertThrows(PersistenceException.class, () -> sut.getMappedField(File.class, "unknownAttribute"));
    }
}