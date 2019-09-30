package cz.cvut.kbss.termit.model.selector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TextPositionSelectorTest {

    @Test
    void twoSelectorsAreEqualWhenPositionsAreEqual() {
        final TextPositionSelector sOne = new TextPositionSelector(117, 145);
        final TextPositionSelector sTwo = new TextPositionSelector(117, 145);
        assertEquals(sOne, sTwo);
        assertEquals(sOne.hashCode(), sTwo.hashCode());
    }

    @Test
    void twoSelectorsAreNotEqualWhenPositionsAreDifferent() {
        final TextPositionSelector sOne = new TextPositionSelector(117, 150);
        final TextPositionSelector sTwo = new TextPositionSelector(117, 145);
        assertNotEquals(sOne, sTwo);
    }
}