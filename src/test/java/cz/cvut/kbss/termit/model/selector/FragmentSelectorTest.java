package cz.cvut.kbss.termit.model.selector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FragmentSelectorTest {

    @Test
    void selectorsAreEqualWhenValueIsSame() {
        final FragmentSelector sOne = new FragmentSelector("http://example.com/image1#xywh=100,100,300,300");
        final FragmentSelector sTwo = new FragmentSelector("http://example.com/image1#xywh=100,100,300,300");
        assertEquals(sOne, sTwo);
        assertEquals(sOne.hashCode(), sTwo.hashCode());
    }

    @Test
    void selectorsWithDifferentValueAreNotEqual() {
        final FragmentSelector sOne = new FragmentSelector("http://example.com/image1#xywh=100,100,300,300");
        final FragmentSelector sTwo = new FragmentSelector("http://example.com/image1#xywh=100,100,300,150");
        assertNotEquals(sOne, sTwo);
    }
}