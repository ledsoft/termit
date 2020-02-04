/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.util;

import cz.cvut.kbss.termit.exception.TermItException;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {
        throw new AssertionError();
    }

    /**
     * Returns an empty collection if the specified collection is {@code null}. Otherwise, the collection itself is
     * returned.
     *
     * @param collection The collection to check
     * @return Non-null collection
     */
    public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return collection == null ? Collections.emptySet() : collection;
    }

    public static String loadQuery(String queryFile) {
        final InputStream is = Utils.class.getClassLoader().getResourceAsStream(
                Constants.QUERY_DIRECTORY + File.separator + queryFile);
        if (is == null) {
            throw new TermItException(
                    "Initialization exception. Query query not found in " + Constants.QUERY_DIRECTORY +
                            File.separator + queryFile);
        }
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            return in.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new TermItException("Initialization exception. Unable to load full text search query!", e);
        }
    }
}
