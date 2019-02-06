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
package com.winkelhagen.chess.frankwalter.engine.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;

public class SANTest {
	Board board = new Board();

	@Test
	public void testPawns() throws IllegalFENException {
		board.setupBoard("rnbqkb1r/pp2p1Pp/8/2ppPp2/3P4/8/PPP2PPP/RNBQKBNR w KQkq f6 0 4");
		assertEquals("a2a4", MV.toString(StaticMoveGenerator.parseSAN(board, "a4")));
		assertEquals("d4c5", MV.toString(StaticMoveGenerator.parseSAN(board, "dxc5")));
		assertEquals("e5f6", MV.toString(StaticMoveGenerator.parseSAN(board, "exf6")));
		assertEquals("g7h8n", MV.toString(StaticMoveGenerator.parseSAN(board, "gxh8=N")));
		assertEquals("g7g8b", MV.toString(StaticMoveGenerator.parseSAN(board, "g8=B")));
	}
	
	@Test
	public void testCheck() throws IllegalFENException {
		board.setupBoard("r1bqk1nr/pppp1ppp/2n5/4p3/1b1PP3/2N5/PPP2PPP/R1BQKBNR w KQkq - 1 4");
		assertEquals("g1e2", MV.toString(StaticMoveGenerator.parseSAN(board, "Ne2")));
	}

	@Test
	public void testNonCheck() throws IllegalFENException {
		board.setupBoard("r1bqk1nr/pppp1ppp/8/4p3/1b1nP3/2N5/PPPB1PPP/R2QKBNR w KQkq - 0 5");
		assertEquals(0, StaticMoveGenerator.parseSAN(board, "Ne2"));
		assertEquals("c3e2", MV.toString(StaticMoveGenerator.parseSAN(board, "Nce2")));
		assertEquals("c3e2", MV.toString(StaticMoveGenerator.parseSAN(board, "N3e2")));
		board.setupBoard("r1bqk1nr/pppp1ppp/8/4p3/1b2P3/2N5/PPPBnPPP/R3KBNR w KQkq - 0 6");
		assertEquals(0, StaticMoveGenerator.parseSAN(board, "Nxe2"));
		assertEquals("c3e2", MV.toString(StaticMoveGenerator.parseSAN(board, "Ncxe2")));
		assertEquals("c3e2", MV.toString(StaticMoveGenerator.parseSAN(board, "N3xe2")));
	}

	@Test
	public void testManyQueens() throws IllegalFENException {
		board.setupBoard("2Q1Q1Q1/8/2Q3Q1/5r2/2Q1Q1Q1/3K4/8/1k6 w - - 0 1");
		assertEquals("c8e6", MV.toString(StaticMoveGenerator.parseSAN(board, "Qc8e6")));
		assertEquals("e4f5", MV.toString(StaticMoveGenerator.parseSAN(board, "Qe4xf5")));
	}
	
	@Test
	public void testThis() throws IllegalFENException {
		board.setupBoard("r1bq2rk/pp3pbp/2p1p1pQ/7P/3P4/2PB1N2/PP3PPR/2KR4 w - -");
		assertEquals("h6h7", MV.toString(StaticMoveGenerator.parseSAN(board, "Qxh7+")));
	}
}
