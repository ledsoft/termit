/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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