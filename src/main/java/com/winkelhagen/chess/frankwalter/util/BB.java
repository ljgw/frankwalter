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
package com.winkelhagen.chess.frankwalter.util;

import java.util.BitSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;

/*
 * This class is a helper class for BitBoards, but also holds some key bitboard returning functions
 * like the move bitboards, and their initialization.
 * 
 * thanks to Gerd Isenberg and Pradu Kannan, Lasse Hansen, etc: see chessprogramming wiki
 */
public class BB {

	private BB(){}
    
    private static Logger logger = LogManager.getLogger();
	
	/* Helper public statics */
	/**
	 * all (0) white (=even) and (1) black (=odd) squares
	 */
	public static long squaresByColor[] = new long[2];
	/**
	 * a 6x6 inner board.
	 */
	public static long inner;
	/**
	 * 1 rank: rank[0] =1st - rank[7] =8th (rank bitboards)
	 */
	public static long rank[] = new long[8];
	/**
	 * 1 file: file[0] =A - file[7] =H (file bitboards)
	 */
	public static long file[] = new long[8];
	/**
	 * 2 or 3 files: file[0] =AB - file 3 = CDE - file[7] =GH (file bitboards)
	 */
	public static long thickFile[] = new long[8];	// any thick file (ab abc bcd cde, etc)
	/**
	 * 1 or 2 file(s): file[0] =B - file 3 = CE - file[7] =G (file bitboards)
	 */
	public static long closeFiles[] = new long[8];
	/**
	 * ranksBetween[i][j]: all ranks between [i] and [j]
	 */
	public static long ranksBetween[][] = new long[8][8]; //all ranks between arg1 and arg2.
	/**
	 * fileInFront[i][j]: the part of file <i>fileOf([j])</i> in front of the piece on [j] of color [i] 
	 */
	public static long fileInFront[][] = new long[2][64];
	/**
	 * thickFileInFront[i][j]: the part of the thick file <i>fileOf([j])</i> in front of the piece on [j] of color [i] 
	 */
	public static long thickFileInFront[][] = new long[2][64]; 

	
	private static int[] directions = {1,7,8,9,-1,-7,-8,-9};
	private static int[][][] lineOfAttack = new int[64][64][]; //int x y z attackee x is attacked by attacker y and over squares z (including x, excluding y)
	
	/**
	 * function to get the intervening squares between an attacker (inclusive) and the piece attacked (exclusive)
	 * @param attackingSquare the square of the attacker
	 * @param attackedSquare the square of the attacked piece
	 * @return the intervening squares excluding the attackedSquare
	 */
	public static int[] getInterveningSquares(int attackingSquare, int attackedSquare){
		return lineOfAttack[attackedSquare][attackingSquare];
	}
	/**
	 * castleMasks[i]: array to modify the castleMask of the board according to the piece moved or captured on square [i]
	 */
	public static final int castleMasks[] = {
		14,15,15,15,12,15,15,13,
		15,15,15,15,15,15,15,15,
		15,15,15,15,15,15,15,15,
		15,15,15,15,15,15,15,15,
		15,15,15,15,15,15,15,15,
		15,15,15,15,15,15,15,15,
		15,15,15,15,15,15,15,15,
		11,15,15,15, 3,15,15, 7
	};
	
	/**
	 * array holding 4 bitboards by the square the king castles to, determining which squares the king passes over.
	 */
	public static long castleSquares[] = new long[64];

	/* the simple piece bitboards */
	/**
	 * king[i]: positions a king on [i] can move to.
	 */
	public static long king[] = new long[64];
	/**
	 * knight[i]: positions a knight on [i] can move to.
	 */
	public static long knight[] = new long[64];
	
	/**
	 * pawn[color][i]: positions a pawn of [color] on [i] can move to.
	 */
	public static long pawn[][] = new long [2][64];

	/*
	 * All squares on the bitboard a rook can go to on an empty board, 
	 * except the last square on a file/rank.
	 */
	private static long fileMask[] = new long[64];
	private static long fileFullMask[] = new long[64]; // Same including the last square.
	private static long rankMask[] = new long[64];
	private static long rankFullMask[] = new long[64]; // Same including the last square.

	private static long fileAttacks[][] = new long[64][64];
	private static long rankAttacks[][] = new long[64][64];

	/*
	 * All squares on the bitboard a bishop can go to on an empty board, 
	 * except the last square on a diagonal.
	 */
	static long[] bishopMask = new long[64];
	private static long bishopFullMask[] = new long[64]; // Same including the last square.

	/* self generated magics */
	static final long[] bishopMagic = {

		   0x4812a00752f9abf8L,0x6a25efecee97ff92L,0x11481203c2019826L,0x4320890304120c00L,
		   0x840c10448420006cL,0x0812038420822015L,0x5360ad562b7f52bdL,0xe9bbd6a7fb2fff7dL,
		   0x5b430ab67999eff8L,0xeccb858fe58a7ff3L,0x202a24040c184520L,0xac025440c0800140L,
		   0x8412840520000402L,0x0829120350481000L,0x6adce911176bff84L,0xb9e16c71a9d1ffa8L,
		   0xaec0384b53ca6fd8L,0x3d60331b45631ff4L,0x1030288800e04010L,0x0408030402162200L,
		   0xd045000820282482L,0x0152028101050304L,0x2304004496135000L,0x0006841a4a02b002L,
		   0x005010060a9a9004L,0x0184031424300420L,0x002190000800c012L,0x0652008028008052L,
		   0x0369003031024002L,0x40ad490046011302L,0x492c911802011049L,0x4502018028442090L,
		   0x0428208410502414L,0x2001480211e00c0aL,0x49d241ae00100406L,0x8223da0080180280L,
		   0x0811010c00160160L,0x1401010a012102c6L,0x0502222615140090L,0x07140284801a10c1L,
		   0x210fe64ab5014055L,0x8907fcfa866d6005L,0x7401210402024044L,0x9024446128001404L,
		   0x300802200a00c100L,0x6a54300462019040L,0x24bfe5931f23dc02L,0xa71f7d567e687a00L,
		   0xcb1ff911e959d28cL,0xf873f9632c395f9bL,0x0811010880904208L,0x8106a21a20980020L,
		   0xa70400115602087cL,0x2a4320080b084260L,0xe07f93aad7fcb043L,0xc77eb509f9b79dcdL,
		   0x753ffcfedcf3eccbL,0xfa9f6ffeb4efa5e8L,0x2051d5c0c4140411L,0xa2e814278884142cL,
		   0x8a401010700a0201L,0x42000e6008011848L,0xfeff7f72faf97bffL,0xfffffa6ffe7e6ef1L};

	static final int[] bishopShift = {
	  59,60,59,59,59,59,60,59,
	  60,60,59,59,59,59,60,60,
	  60,60,57,57,57,57,59,59,
	  59,59,57,55,55,57,59,59,
	  59,59,57,55,55,57,59,59,
	  60,60,57,57,57,57,60,60,
	  60,60,59,59,59,59,60,60,
	  59,60,59,59,59,59,60,59};

	private static long bishop[][] = {
	  new long[32],new long[16],new long[32],new long[32],new long[32],new long[32],new long[16],new long[32],
	  new long[16],new long[16],new long[32],new long[32],new long[32],new long[32],new long[16],new long[16],
	  new long[16],new long[16],new long[128],new long[128],new long[128],new long[128],new long[32],new long[32],
	  new long[32],new long[32],new long[128],new long[512],new long[512],new long[128],new long[32],new long[32],
	  new long[32],new long[32],new long[128],new long[512],new long[512],new long[128],new long[32],new long[32],
	  new long[16],new long[16],new long[128],new long[128],new long[128],new long[128],new long[16],new long[16],
	  new long[16],new long[16],new long[32],new long[32],new long[32],new long[32],new long[16],new long[16],
	  new long[32],new long[16],new long[32],new long[32],new long[32],new long[32],new long[16],new long[32]};
/* end of the public longs! */


/* Helper functions for the initialization of the slider bitboards */
	static boolean fillBishopBitboards(long done, long todo, long magic, int shift, int sq, boolean resultSet[], long resultLng[]){
		if (todo==0){
			long attacks = bishopAttacks(1L << sq, (todo | done));
			int result = applyBishopMagic(todo | done, magic, shift);
			bishop[sq][result] = attacks;
			if (resultSet[result] == true){
				if (resultLng[result] != attacks){
					return false;
				}
			} else {
				resultLng[result] = attacks;
				resultSet[result] = true;
			}
		} else {
			long twiddleBit = todo & ( - todo);
			if (!fillBishopBitboards(done|twiddleBit, todo ^ twiddleBit, magic, shift, sq, resultSet, resultLng)) return false;
			if (!fillBishopBitboards(done, todo ^ twiddleBit, magic, shift, sq, resultSet, resultLng)) return false;
		}
		return true;
	}
	private static int applyBishopMagic(long occ, long magic, int shift){
        occ      *= magic;
	    occ     >>>= shift;
		return (int)(occ);
	}
 
	private static boolean fillFileBitboards(long done, long todo, int sq, boolean resultSet[], long resultLng[]){
		if (todo==0){
			long attacks = fileAttacks(1L << sq, (todo | done));
			int result = applyFileMagic(todo | done);
			fileAttacks[sq][result] = attacks;
			if (resultSet[result] == true || attacks==0){
				return false;
			} else {
				resultLng[result] = attacks;
				resultSet[result] = true;
			}
		} else {
			long twiddleBit = todo & ( - todo);
			if (!fillFileBitboards(done|twiddleBit, todo ^ twiddleBit, sq, resultSet, resultLng)) return false;
			if (!fillFileBitboards(done, todo ^ twiddleBit, sq, resultSet, resultLng)) return false;
		}
		return true;
	}
	private static int applyFileMagic(long occ) {
	   long fold  = (int)occ | (int)((occ >>> 32) << 3) ;
	   int occ64 = (int)(fold * 0x01041041) >>> 26;
	   return occ64;
	}
	private static boolean fillRankBitboards(long done, long todo, int sq, boolean resultSet[], long resultLng[]){
		if (todo==0){
			long attacks = rankAttacks(1L << sq, (todo | done));
			int result = applyRankMagic(todo | done, sq);
			rankAttacks[sq][result] = attacks;
			if (resultSet[result] == true || attacks==0){
				return false;
			} else {
				resultLng[result] = attacks;
				resultSet[result] = true;
			}
		} else {
			long twiddleBit = todo & ( - todo);
			if (!fillRankBitboards(done|twiddleBit, todo ^ twiddleBit, sq, resultSet, resultLng)) return false;
			if (!fillRankBitboards(done, todo ^ twiddleBit, sq, resultSet, resultLng)) return false;
		}
		return true;
	}
	static int applyRankMagic(long occ, int sq) {
		int occ64 = (int)(occ >>> ((sq>>3)*8+1));
		return occ64;
	}


/* Initialization function^^ */
	static {
		//Castle stuff
		castleSquares[2] = 3L << 3;
		castleSquares[6] = 3L << 4;
		castleSquares[58] = 3L << 59;
		castleSquares[62] = 3L << 60;

		//file and rank stuff
		inner = ~(BB.file(0) | BB.file(7) | BB.rank(0) | BB.rank(7));
		for (int i=0; i<8; i++){
			rank[i] = rank(i);
			file[i] = file(i);
			thickFile[i] = file(i);
			if (i!=0){
				thickFile[i] |= file(i-1);
			}
			if (i!=7){
				thickFile[i] |= file(i+1);
			}
			closeFiles[i] = thickFile[i] & ~file[i];
		}
		for (int i=0; i<8; i++){
			for (int j=i; j<8; j++){
				for (int k=i; k<j+1;k++){
					ranksBetween[i][j] |= rank[k];
				}
			}
		}
		for (int rank = 1; rank<7; rank ++){
			for (int file = 0; file<8; file++){
				pawn[0][rank*8 + file] = ranksBetween[rank+1][rank+1] & closeFiles[file];
				pawn[1][rank*8 + file] = ranksBetween[rank-1][rank-1] & closeFiles[file];
			}
		}
		for (int i=0; i<64; i++){
			if (rankOf(i)!=7){
				thickFileInFront[0][i] = thickFile[fileOf(i)] & ranksBetween[rankOf(i)+1][7];
				fileInFront[0][i] = file[fileOf(i)] & ranksBetween[rankOf(i)+1][7];
			}
			if (rankOf(i)!=0){
				thickFileInFront[1][i] = thickFile[fileOf(i)] & ranksBetween[0][rankOf(i)-1];
				fileInFront[1][i] = file[fileOf(i)] & ranksBetween[0][rankOf(i)-1];
			}
		}

		//color of squares
		for (int i=0; i<64; i++){
			squaresByColor[Square.color(i)] |= single(i);
		}

		//pieces
		for (int i=0; i<64; i++){
			king[i] = kingMoves(i);
			knight[i] = knightMoves(i);
		}
		
		for (int i=0; i<64;i++){
			int range = 1 << (64-bishopShift[i]);
			boolean resultSet[] = new boolean[range];
			long resultLng[] = new long[range];
			bishopFullMask[i] = BB.bishopAttacks(1L << i, (1L << i));
			long mask = bishopFullMask[i] & inner;
			bishopMask[i] = mask;
			if (!fillBishopBitboards(0L, mask, bishopMagic[i], bishopShift[i], i, resultSet, resultLng)){
			    logger.error("Failure Detected! Generating bishop moves for square: {}.", Square.byNumber(i));
			}
		}
		for (int i=0; i<64;i++){
			int range = 64;
			boolean resultSet[] = new boolean[range];
			long resultLng[] = new long[range];
			for (int j = 0; j<range; j++){
				resultSet[j] = false;
			}
			fileFullMask[i] = BB.fileAttacks(1L << i, (1L << i));
			long mask = fileFullMask[i] & ~(rank[0]|rank[7]);
			fileMask[i] = mask;
			if (!fillFileBitboards(0L, mask, i, resultSet, resultLng)){
			    logger.error("Failure Detected! Generating rook moves for square: {}.", Square.byNumber(i));
			}
		}
		for (int i=0; i<64;i++){
			int range = 64;
			boolean resultSet[] = new boolean[range];
			long resultLng[] = new long[range];
			for (int j = 0; j<range; j++){
				resultSet[j] = false;
			}
			rankFullMask[i] = BB.rankAttacks(1L << i, (1L << i));
			long mask = rankFullMask[i] & ~(file[0]|file[7]);
			rankMask[i] = mask;
			if (!fillRankBitboards(0L, mask, i, resultSet, resultLng)){
			    logger.error("Failure Detected! Generating rook moves for square: {}.", Square.byNumber(i));
			}
		}
		
		//Intervening square tables.
		for (int attackee=0; attackee<64; attackee++){
			long knightMoves = knight[attackee];
			while (knightMoves!=0){
				int attacker = lsb(knightMoves);
				lineOfAttack[attackee][attacker] = new int[1];
				lineOfAttack[attackee][attacker][0] = attacker; 
				knightMoves^=single(attacker);
			}
			for (int dir : directions){
				if (dir>0){
					long slidingAttacks = slidingAttacks(single(attackee), single(attackee), dir);
					while (slidingAttacks!=0){
						int attacker = msb(slidingAttacks);
						slidingAttacks ^= single(attacker);
						long attacksLeft = slidingAttacks;
						lineOfAttack[attackee][attacker] = new int[Long.bitCount(attacksLeft)+1];
						int i = 0;
						lineOfAttack[attackee][attacker][i++] = attacker;
						while (attacksLeft!=0){
							int interveningSquare = msb(attacksLeft);
							lineOfAttack[attackee][attacker][i++] = interveningSquare;
							attacksLeft ^= single(interveningSquare);
						}
					}
				} else {
					long slidingAttacks = slidingAttacks(single(attackee), single(attackee), dir);
					while (slidingAttacks!=0){
						int attacker = lsb(slidingAttacks);
						slidingAttacks ^= single(attacker);
						long attacksLeft = slidingAttacks;
						lineOfAttack[attackee][attacker] = new int[Long.bitCount(attacksLeft)+1];
						int i = 0;
						lineOfAttack[attackee][attacker][i++] = attacker;
						while (attacksLeft!=0){
							int interveningSquare = lsb(attacksLeft);
							lineOfAttack[attackee][attacker][i++] = interveningSquare;
							attacksLeft ^= single(interveningSquare);
						}
					}
				}
			}
		}
	}
	/* get index for sliders functions */

	/**
	 * get the horizontal and vertical all moves bitboard for a certain square and occupancy</br>
	 * This uses Kindergarten Bitboards for verticals and plain bitboards for horizontals.
	 * @param square the square the piece is on
	 * @param occupied the relevant occupancy
	 * @return a bitboard with horizontally and vertically attacked squares
	 */
	public static long getRookMoves(int square, long occupied) {
		long rankBB = rankAttacks[square][(int)((occupied & rankMask[square]) >>> ((square>>3)*8+1))];
		occupied &= fileMask[square];
		int fold  = (int)occupied | (int)((occupied >>> 32) << 3);
		int occ64 = (int)(fold * 0x01041041) >>> 26;
		return fileAttacks[square][occ64] | rankBB ;
	}

	/**
	 * get the diagonal all moves bitboard for a certain square and occupancy</br>
	 * This uses magic bitboards.
	 * @param square the square the piece is on
	 * @param occupied the relevant occupancy
	 * @return a bitboard with diagonally attacked squares
	 */
	public static long getBishopMoves(int square, long occupied){
		occupied	  &= bishopMask [square];	
		occupied	  *= bishopMagic[square];
		occupied	>>>= bishopShift[square];
		return bishop[square][(int)(occupied)];
	}

	/* Basic operations */
	/**
	 * function to get the rank (0-7) of a square using square >> 3.
	 * @param square the square
	 * @return the rank
	 */
	public static int rankOf(int square){
		return square>>3;
	}
	
	/**
	 * function to get the file (0-7) of a square.
	 * square % 8 (or & 7)
	 * @param square the square
	 * @return the file
	 */
	public static int fileOf(int square){
		return square%8;
	}
	
	/**
	 * function to get a single bitboard (long) of a square.
	 * 1L << square
	 * @param square the square
	 * @return the bitboard
	 */
	public static long single(int square){
		return 1L << square;
	}

	/**
	 * function to add a square to a bitboard (long).
	 * bb | 1L << square
	 * @param square the square
	 * @param bb the bitboard
	 * @return the resulting bitboard
	 */
	public static long add(long bb, int square){
		return bb | 1L << square;
	}

	/**
	 * generate a BitSet from a bitboard
	 * @param bb the bitboard
	 * @return the BitSet
	 */
	public static BitSet toBitSet(long bb){
		BitSet bs = new BitSet(64);
		while (bb!=0){
			bs.set(Long.numberOfTrailingZeros(bb));
			bb &= bb - 1;
		}
		return bs;
	}
	/**
	 * Least Significant Bit 
	 * @param bb the bitboard
	 * @return the number of the least significant bit
	 */
	public static int lsb(long bb){
		return Long.numberOfTrailingZeros(bb);
	}
	/**
	 * Most Significant Bit 
	 * @param bb the bitboard
	 * @return the number of the most significant bit
	 */
	public static int msb(long bb){
		return 63-Long.numberOfLeadingZeros(bb);
	}
	/**
	 * Strip Least Significant Bit (bb &= bb - 1;)
	 * @param bb the bitboard
	 * @return the resulting bitboard
	 */
	public static long nextBit(long bb){
		bb &= bb - 1;
		return bb;
	}

	/**
	 * displays one bitboard
	 * @param bb the bitboard
	 */
	public static void display(long bb){
	    StringBuilder sb = new StringBuilder();
		for (int i = 0; i<8;i++){
			for (int j = 0; j<8;j++){
				if ((bb & single(i*8 + j))==0){
					sb.append("0 ");
				} else {
					sb.append("1 ");
				}
			}
			sb.append("\n");
		}
		OutputPrinter.printOutput(sb.toString());
	}

	/**
	 * displays two bitboards
	 * @param bb1 bitboard 1
	 * @param bb2 bitboard 2
	 */
	public static void display(long bb1, long bb2){
	    StringBuilder sb = new StringBuilder();
		for (int i = 0; i<8;i++){
			for (int j = 0; j<8;j++){
				if ((bb1 & single(i*8 + j))==0){
					sb.append("0 ");
				} else {
					sb.append("1 ");
				}
			}
			sb.append("<-> ");
			for (int j = 0; j<8;j++){
				if ((bb2 & single(i*8 + j))==0){
					sb.append("0 ");
				} else {
					sb.append("1 ");
				}
			}
			sb.append("\n");
		}
		OutputPrinter.printOutput(sb.toString());
	}

	private static long rank(int i){
		long bb = 255L;
		return bb << (i*8);
	}

	private static long file(int i){
		long bb = 1L;
		for (int j=1; j<8; j++){
			bb |= 1L << (j*8);
		}
		return bb << i;
	}

	static long bishopAttacks(long sliders, long empty){
		return slidingAttacks(sliders, empty, 9) | slidingAttacks(sliders, empty, -7) | slidingAttacks(sliders, empty, -9) | slidingAttacks(sliders, empty, 7);
	}
	static long rookAttacks(long sliders, long empty){
		return slidingAttacks(sliders, empty, 1) | slidingAttacks(sliders, empty, -1) | slidingAttacks(sliders, empty, -8) | slidingAttacks(sliders, empty, 8);
	}
	private static long fileAttacks (long sliders, long empty){
		return slidingAttacks(sliders, empty, -8) | slidingAttacks(sliders, empty, 8);
	}
	private static long rankAttacks (long sliders, long empty){
		return slidingAttacks(sliders, empty, -1) | slidingAttacks(sliders, empty, 1);
	}

	private static long slidingAttacks (long sliders, long empty, int dir8)
	{	
		int sq = Long.numberOfTrailingZeros(sliders);
		long bb = 0L;
		switch (dir8){
		case (-9):
			for (int i = sq; rankOf(i)!=0 && fileOf(i)!=0; i-=9){
				bb |= 1L << (i-9);
				if ((empty & bb)!=0) break;
			}
			break;
		case (-8):
			for (int i = sq; rankOf(i)!=0; i-=8){
				bb |= 1L << (i-8);
				if ((empty & bb)!=0) break;
			}
			break;
		case (-7):
			for (int i = sq; rankOf(i)!=0 && fileOf(i)!=7; i-=7){
				bb |= 1L << (i-7);
				if ((empty & bb)!=0) break;
			}
			break;
		case (-1):
			for (int i = sq; fileOf(i)!=0; i-=1){
				bb |= 1L << (i-1);
				if ((empty & bb)!=0) break;
			}
			break;
		case (9):
			for (int i = sq; rankOf(i)!=7 && fileOf(i)!=7; i+=9){
				bb |= 1L << (i+9);
				if ((empty & bb)!=0) break;
			}
			break;
		case (8):
			for (int i = sq; rankOf(i)!=7; i+=8){
				bb |= 1L << (i+8);
				if ((empty & bb)!=0) break;
			}
			break;
		case (7):
			for (int i = sq; rankOf(i)!=7 && fileOf(i)!=0; i+=7){
				bb |= 1L << (i+7);
				if ((empty & bb)!=0) break;
			}
			break;
		case (1):
			for (int i = sq; fileOf(i)!=7; i+=1){
				bb |= 1L << (i+1);
				if ((empty & bb)!=0) break;
			}
			break;
		}
		return bb;
	}
	
	private static long kingMoves(int i){
		int j = i + rankOf(i)*8;
		long bb = 0L;
		if ((j+1  & 0x88)==0) {bb |= single(i+1);}
		if ((j+15 & 0x88)==0) {bb |= single(i+7);}
		if ((j+16 & 0x88)==0) {bb |= single(i+8);}
		if ((j+17 & 0x88)==0) {bb |= single(i+9);}
		if ((j-1  & 0x88)==0) {bb |= single(i-1);}
		if ((j-15 & 0x88)==0) {bb |= single(i-7);}
		if ((j-16 & 0x88)==0) {bb |= single(i-8);}
		if ((j-17 & 0x88)==0) {bb |= single(i-9);}
		return bb;
	}

	private static long knightMoves(int i){
		int j = i + rankOf(i)*8;
		long bb = 0L;
		if ((j+33 & 0x88)==0) {bb |= single(i+17);}
		if ((j+31 & 0x88)==0) {bb |= single(i+15);}
		if ((j+18 & 0x88)==0) {bb |= single(i+10);}
		if ((j+14 & 0x88)==0) {bb |= single(i+6) ;}
		if ((j-33 & 0x88)==0) {bb |= single(i-17);}
		if ((j-31 & 0x88)==0) {bb |= single(i-15);}
		if ((j-18 & 0x88)==0) {bb |= single(i-10);}
		if ((j-14 & 0x88)==0) {bb |= single(i-6) ;}
		return bb;
	}

}