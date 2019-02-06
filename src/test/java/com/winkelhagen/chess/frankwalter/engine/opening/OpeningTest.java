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
package com.winkelhagen.chess.frankwalter.engine.opening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;
import org.junit.Test;

import com.winkelhagen.chess.frankwalter.board.Board;

public class OpeningTest {

	@Test
	public void testImport(){
		Book book = new SimpleBookImpl();
		assertTrue(book.loadBook("frankwalter.openings"));
	}
	
	@Test
	public void testZobrist() throws IllegalFENException {
		Book book = new SimpleBookImpl();
		assertTrue(book.loadBook("frankwalter.openings"));
		Board board = new Board();
		board.setupBoard("rnbqkb1r/pp2pppp/3p1n2/8/3NP3/8/PPP2PPP/RNBQKB1R w KQkq -");
		assertEquals("b1c3", MV.toString(book.probeBook(board.getHashKey())));
	}
}
