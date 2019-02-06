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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.board.BoardView;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import org.junit.Test;

import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;


public class BoardTest {
	
	@Test
	public void testFEN() throws IllegalFENException {
		Board board = new Board();
		BoardView boardView = new BoardView(board);
		String testPosition = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w Qk c6 2 2";
		board.setupBoard(testPosition);
		assertEquals(testPosition, boardView.getFEN(false));
	}
	
	@Test
	public void testPins() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1k1nr/pppp1ppp/8/4p3/1b2P2q/2NP4/PPP2PPP/R1BQKBNR w KQkq - 1 4");
		assertEquals(2,Long.bitCount(board.getPins(Constants.WHITE)));
		board.doMove(MV.toBasicMove("g2g3"));
		assertEquals(1,Long.bitCount(board.getPins(Constants.WHITE)));
		board.doMove(MV.toBasicMove("b4c3"));
		assertEquals(0,Long.bitCount(board.getPins(Constants.WHITE)));
		
		
	}
}
