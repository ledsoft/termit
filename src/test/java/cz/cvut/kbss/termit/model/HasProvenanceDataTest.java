package cz.cvut.kbss.termit.model;

import cz.cvut.kbss.termit.environment.Generator;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HasProvenanceDataTest {

    @Test
    void getLastModifiedOrCreatedReturnsLastModifiedWhenItIsNotNull() {
        final Term asset = Generator.generateTermWithId();
        asset.setCreated(new Date(System.currentTimeMillis() - 1000));
        asset.setLastModified(new Date());
        assertEquals(asset.getLastModified(), asset.getLastModifiedOrCreated());
    }

    @Test
    void getLastModifiedOrCreatedReturnsCreatedWhenLastModifiedIsNull() {
        final Term asset = Generator.generateTermWithId();
        asset.setCreated(new Date(System.currentTimeMillis() - 1000));
        assertEquals(asset.getCreated(), asset.getLastModifiedOrCreated());
    }
}