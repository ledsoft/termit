package cz.cvut.kbss.termit.model.selector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class XPathSelectorTest {

    @Test
    void selectorsAreEqualWhenValueIsSame() {
        final XPathSelector sOne = new XPathSelector("//div[class=\'test\']");
        final XPathSelector sTwo = new XPathSelector("//div[class=\'test\']");
        assertEquals(sOne, sTwo);
        assertEquals(sOne.hashCode(), sTwo.hashCode());
    }

    @Test
    void selectorsWithDifferentValueAreNotEqual() {
        final XPathSelector sOne = new XPathSelector("//div[class=\'test\']");
        final XPathSelector sTwo = new XPathSelector("//div[class=\'test-different\']");
        assertNotEquals(sOne, sTwo);
    }
}