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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.board.BoardView;
import com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import org.junit.Test;

import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;

public class MoveTest {

	
	private List<String> movesToList(int[] moves, int movesNr){
		List<String> list = new ArrayList<String>();
		for (int i=0; i<movesNr; i++){
			list.add(MV.toString(moves[i]));
		}
		return list;
	}
		
	@Test
	public void testLegalMoves_STARTPOS() throws IllegalFENException {
		Board board = new Board();
		board.setupBoard(Constants.STARTPOS);
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(20, moveList.size());
		assertTrue(moveList.contains("e2e4"));
	}

	@Test
	public void testLegalMoves() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("r3kbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQK2R w Kq f6 0 3");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(27, moveList.size());
	}

	@Test
	public void testLegalMoves_PINS() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1k1nr/pppp1ppp/8/4p3/1b2P2q/2NP4/PPP2PPP/R1BQKBNR w KQkq - 1 4");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(24, moveList.size());
	}
	
	@Test
	public void testLegalMoves_PINS_2() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1k1nr/pppp1ppp/8/b2Np3/4P1q1/3P4/PPPB1PPP/R2QKBNR w KQkq - 5 3");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(34, moveList.size());
		assertTrue(moveList.contains("d2a5"));
		assertFalse(moveList.contains("e1e2"));
	}
	
	@Test
	public void testLegalMoves_PROMO() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqkbnr/ppp1ppPp/8/8/8/8/PPpPP1PP/RNBQKBNR w KQkq - 0 5");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(26, moveList.size());
		assertTrue(moveList.contains("g7h8n"));
		assertTrue(moveList.contains("g7f8r"));
	}
	
	@Test
	public void testLegalMoves_PROMOPIN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("1nb1qbnr/rpkpPppp/p1p5/8/5p2/8/PPPP2PP/RNBQKBNR w KQ - 1 10");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(28, moveList.size());
		assertFalse(moveList.contains("e7f8n"));
	}

	@Test
	public void testLegalMoves_PINNED_KILLER_PAWN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnbqk1nr/pppp1ppp/4p3/8/8/2b2N1P/PPPPPPP1/R1BQKB1R w KQkq - 0 4");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(20, moveList.size());
		assertTrue(moveList.contains("d2c3"));
	}

	@Test
	public void testLegalMoves_PINNED_BLACK_QUEEN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("rnb1kb1r/pppqpppp/5n2/1B1p4/8/2N1PN2/PPPP1PPP/R1BQK2R b KQkq - 3 4");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(22, moveList.size());
	}
	
	@Test
	public void testLegalMoves_PINNED_KILLER_PAWN2() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("8/2p5/3p4/KP5r/1R3p1k/6P1/4P3/8 b - - 0 1");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateOutOfCheckMoves(board, StaticMoveGenerator.getKingAttacker(board, board.getSideToMove()^1), atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(4, moveList.size());
		assertFalse(moveList.contains("f4g3"));
	}

	@Test
	public void testLegalMoves_ODDITY() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("8/2p5/3p1R2/KP5r/8/6k1/4P1P1/8 b - - 2 2");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateMoves(board, atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertEquals(20, moveList.size());
	}

	@Test
	public void testLegalMoves_EP_KILLER_PAWN() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("8/8/3p4/1Pp4r/1K3R2/6k1/4P1P1/8 w - c6 0 3");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateOutOfCheckMoves(board, StaticMoveGenerator.getKingAttacker(board, board.getSideToMove()^1), atmMoves);
		

		List<String> moveList = movesToList(atmMoves, atmNr);
		assertTrue(moveList.contains("b5c6"));
		assertEquals(7, moveList.size());
	}
	
	@Test
	public void testLegalMoves_STUPID_KING() throws IllegalFENException{
		Board board = new Board();
		board.setupBoard("8/6pp/4k3/8/7P/8/pK1r2P1/8 w - - 2 33");
		
		int[] atmMoves = new int[100];
		int atmNr = StaticMoveGenerator.generateOutOfCheckMoves(board, StaticMoveGenerator.getKingAttacker(board, board.getSideToMove()^1), atmMoves);
		
		List<String> moveList = movesToList(atmMoves, atmNr);
		assertFalse(moveList.contains("b2a2"));
		assertEquals(5, moveList.size());
	}

	@Test
	public void testKingInCheckMoves() throws IllegalFENException{
		Board board = new Board();
		BoardView boardView = new BoardView(board);
		board.setupBoard("8/p7/8/8/4p3/1k1PP3/P6p/6K1 b KQkq - 0 1");
		boardView.echoPosition();
		assertTrue(StaticMoveGenerator.isKingAttacked(board,board.getSideToMove()));
	}
}
