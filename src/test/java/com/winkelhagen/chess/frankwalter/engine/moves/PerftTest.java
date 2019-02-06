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

import org.junit.Test;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.Constants;

/**
 * test move generation and move execution (and the speed)
 */
public class PerftTest {

	private int movesTable[][] = new int[100][100];
	
	private static final String[] positions = {
			Constants.STARTPOS,
			"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -",
			"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -",
			"8/8/4r3/3b4/4kp1R/8/5BPK/8 w - - 0 2",
			Constants.STARTPOS,
			"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -",
			"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -",
			"8/8/4r3/3b4/4kp1R/8/5BPK/8 w - - 0 2"
	};
	private static final long[][] expectedNodes = {
			{20,400,8902,197281,4865609,119060324},
			{48,2039,97862,4085603,193690690},
			{14,191,2812,43238,674624,11030083,178633661},
			{20,372,7546,163237,3348915,76373521},
			{20,400,8902,197281,4865609,119060324},
			{48,2039,97862,4085603,193690690},
			{14,191,2812,43238,674624,11030083,178633661},
			{20,372,7546,163237,3348915,76373521}
	};
	
	private Board board = new Board();


	private long perft(int depth){
		long nodeCount = 0;
		int moves[] = movesTable[depth];
		if (depth==0) return 1;
		int movesNr = StaticMoveGenerator.generateLegalMoves(board, moves);
		if (depth==1){
			return movesNr;
		}
		for (int i=0; i<movesNr; i++){
			board.doMove(moves[i]);
			long returned = perft(depth-1);
			if (returned!=-1){
				nodeCount += returned;
			}
			board.undoMove();
		}
		return nodeCount;
	}
	
	@Test
	public void testPerft() throws IllegalFENException{
		int positionCount = 0;
		for (String fen : positions){
			board.setupBoard(fen);
			System.out.println("POSITION: (" + positionCount +") " + fen);
			int depth = 0;
			for (int i = 0; i<expectedNodes[positionCount].length- depth; i++){
				if (positionCount!=2) break;
				long startTime = System.nanoTime();
				long nodes = perft(i+1);
				long duration = System.nanoTime() - startTime;
				System.out.println("(depth = "+i+") nodes: "+ nodes + " in " + duration + " nanos, " + 1000*nodes/duration + " M-nodes per second");
				assertEquals(expectedNodes[positionCount][i],nodes);
			}
			positionCount++;
		}
	}

	//below testcases come form TalkChess PERFT Tests (by Martin Sedlak) via https://www.chessprogramming.net/perfect-perft/

	@Test
	public void testIllegalEP1() throws IllegalFENException {
		String FEN = "3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1134888, perft(6));
	}

	@Test
	public void testIllegalEP2() throws IllegalFENException {
		String FEN = "8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1015133, perft(6));
	}

	@Test
	public void testIllegalEPChecks() throws IllegalFENException {
		String FEN = "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1440467, perft(6));
	}

	@Test
	public void testShortCastleChecks() throws IllegalFENException {
		String FEN = "5k2/8/8/8/8/8/8/4K2R w K - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 661072, perft(6));
	}

	@Test
	public void testLongCastleChecks() throws IllegalFENException {
		String FEN = "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 803711, perft(6));
	}

	@Test
	public void testCastleRights() throws IllegalFENException {
		String FEN = "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1274206, perft(4));
	}

	@Test
	public void testCastlePrevented() throws IllegalFENException {
		String FEN = "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1720476, perft(4));
	}

	@Test
	public void testPromoteOutOfCheck() throws IllegalFENException {
		String FEN = "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 3821001, perft(6));
	}

	@Test
	public void testDiscoveredCheck() throws IllegalFENException {
		String FEN = "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 1004658, perft(5));
	}

	@Test
	public void testPromoteToGiveCheck() throws IllegalFENException {
		String FEN = "4k3/1P6/8/8/8/8/K7/8 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 217342, perft(6));
	}

	@Test
	public void testUnderPromoteToGiveCheck() throws IllegalFENException {
		String FEN = "8/P1k5/K7/8/8/8/8/8 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 92683, perft(6));
	}

	@Test
	public void testSelfStalemate() throws IllegalFENException {
		String FEN = "K1k5/8/P7/8/8/8/8/8 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 2217, perft(6));
	}

	@Test
	public void testStalemateAndCheckmate1() throws IllegalFENException {
		String FEN = "8/k1P5/8/1K6/8/8/8/8 w - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 567584, perft(7));
	}

	@Test
	public void testStalemateAndCheckmate2() throws IllegalFENException {
		String FEN = "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1";
		board.setupBoard(FEN);
		assertEquals("perft does not return correct number of moves", 23527, perft(4));
	}

}
