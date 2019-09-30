package cz.cvut.kbss.termit.model.selector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CssSelectorTest {

    @Test
    void selectorsAreEqualWhenValueIsSame() {
        final CssSelector sOne = new CssSelector("div.test");
        final CssSelector sTwo = new CssSelector("div.test");
        assertEquals(sOne, sTwo);
        assertEquals(sOne.hashCode(), sTwo.hashCode());
    }

    @Test
    void selectorsWithDifferentValueAreNotEqual() {
        final CssSelector sOne = new CssSelector("div.test");
        final CssSelector sTwo = new CssSelector("div.test-different");
        assertNotEquals(sOne, sTwo);
    }
}