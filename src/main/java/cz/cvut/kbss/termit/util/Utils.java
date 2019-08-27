package cz.cvut.kbss.termit.util;

import java.util.Collection;
import java.util.Collections;

public class Utils {

    /**
     * Returns an empty collection if the specified collection is {@code null}. Otherwise, the collection itself is
     * returned.
     *
     * @param collection The collection to check
     * @return Non-null collection
     */
    public static  <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return collection == null ? Collections.emptySet() : collection;
    }
}
