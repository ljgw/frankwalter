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
package com.winkelhagen.chess.frankwalter.engine.evaluator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;

public class SeeTest {

	@Test
	public void testSeeQxPRxQ() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkbnr/ppppppp1/8/7p/4P3/8/PPPP1PPP/RNBQKBNR w KQkq h6 0 2");
		int move = MV.toBasicMove("d1h5");
		assertEquals("Queen takes pawn, rook takes queen",-8, Seer.see(move, board));
	}

	@Test
	public void testSeeBxP() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkbnr/ppppppp1/8/7p/4P3/8/PPPPBPPP/RNBQK1NR w KQkq h6 0 2");
		int move = MV.toBasicMove("e2h5");
		assertEquals("Bishop takes pawn", 1, Seer.see(move, board));
	}

	@Test
	public void testSeeBxPNxB() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkb1r/ppppppp1/5n2/7p/4P3/8/PPPPBPPP/RNBQK1NR w KQkq h6 0 2");
		int move = MV.toBasicMove("e2h5");
		assertEquals("Bishop takes pawn, knight takes bishop", -2, Seer.see(move, board));
	}

	@Test
	public void testSeePxP() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkb1r/ppppppp1/5n2/7p/4P1P1/8/PPPPBP1P/RNBQK1NR w KQkq h6 0 1");
		int move = MV.toBasicMove("g4h5");
		assertEquals("Pawn takes pawn", 1, Seer.see(move, board));
	}

	@Test
	public void testSeePxPBxPNxBQxN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkb1r/ppppppp1/5n2/7p/4P1P1/8/PPPPBP1P/RNBQK1NR b KQkq g3 0 1");
		int move = MV.toBasicMove("h5g4");
		assertEquals("Pawn takes pawn, full exchange", 0, Seer.see(move, board));
	}

	@Test
	public void testSeePxP2() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1kbr1/pppppppq/5n2/7p/4P1P1/8/PPPPQP1P/RNBBK1NR w KQq - 0 1");
		int move = MV.toBasicMove("g4h5");
		assertEquals("Pawn takes pawn", 1, Seer.see(move, board));
	}

	@Test
	public void testSeeHiddenAttacterSameColor() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1kb1r/pppppppq/5n2/7p/4P1P1/8/PPPPQP1P/RNBBK1NR w KQq - 0 1");
		int move = MV.toBasicMove("g4h5");
		assertEquals("Pawn takes pawn, full exchange", 0, Seer.see(move, board));
	}

	@Test
	public void testSeePxN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1kbr1/pppppppq/8/7n/4P1P1/8/PPPPQP1P/RNB1KBNR w KQq - 0 1");
		int move = MV.toBasicMove("g4h5");
		assertEquals("Pawn takes knight", 3, Seer.see(move, board));
	}

	@Test
	public void testSeeHiddenAttacterDifferentColor() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("r2qkbr1/ppppppp1/4n3/7p/4P1P1/2Q5/PPPPbP1P/RNB1K1NR w KQq - 0 1");
		int move = MV.toBasicMove("g4h5");
		assertEquals("Pawn takes pawn, full exchange", 0, Seer.see(move, board));
	}

}
