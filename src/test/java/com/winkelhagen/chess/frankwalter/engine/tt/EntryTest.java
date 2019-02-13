/**
 * FrankWalter - a java chess engine
 * Copyright © 2019 Laurens Winkelhagen (ljgw@users.noreply.github.com)
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.winkelhagen.chess.frankwalter.engine.tt;

import org.junit.Test;

import static org.junit.Assert.*;

public class EntryTest {
    @Test
    public void entryTest() throws Exception {
        long entry = Entry.toLong(Entry.correctMateScore(-31998,1), (short)1, 1, Entry.EXACT);
        assertEquals("move = ", 1, Entry._move(entry));
        assertEquals("score = -31998", -31998, Entry._score(entry, 1));
        assertEquals("depth = 1", 1, Entry._depth(entry));
        assertEquals("move = 1", Entry.EXACT, Entry._type(entry));
    }

}