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
package com.winkelhagen.chess.frankwalter;

import org.junit.Test;

import com.winkelhagen.chess.frankwalter.engine.tt.Entry;
import com.winkelhagen.chess.frankwalter.util.MV;

import static org.junit.Assert.assertEquals;

public class TTTest {

	@Test
	public void testEntry(){
		Entry entry = new Entry();
		entry.depth = Entry.todepth((short)128, Entry.FAIL_HIGH);
		entry.move = MV.toBasicMove("e2e4");
		entry.score = (short)-99;
		long entryLong = entry.asLong();
		assertEquals("move should be correct", "e2e4", MV.toString(Entry._move(entryLong)));
		assertEquals("score should be correct", -99, Entry._score(entryLong, 23));
		assertEquals("depth should be correct", 128, Entry._depth(entryLong));
		assertEquals("type should be correct", Entry.FAIL_HIGH, Entry._type(entryLong));
	}
}
