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

//import java.util.BitSet;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class BitBoardTest {


	private int controlCount = 0;
	private Random rng = new MersenneTwister();

	private boolean doFindMagic(boolean progressive, long rand, int sq, long[] magics){
		int shift = BB.bishopShift[sq];
		int range = 1 << (64-shift);
		boolean resultSet[] = new boolean[range];
		long resultLng[] = new long[range];
		if (progressive){
			if (BB.fillBishopBitboards(0L, BB.bishopMask[sq], rand, shift+1, sq, resultSet, resultLng)){
				System.out.println("Succes! magic: 0x" + Long.toHexString(rand) + "L & shift: " + (shift+1) + " & square: " + sq);
				magics[sq]=rand;
				return true;
			}
		} else {
			if (BB.fillBishopBitboards(0L, BB.bishopMask[sq], rand, shift, sq, resultSet, resultLng)){
				magics[sq]=rand;
				return true;
			}
		}
		return false;
	}

	@Test
	@Ignore
	/**
	 * this 'test' finds better (=progressive = true) or equal magics for magic bitboard bishop moves.
	 */
	public void testFindMagic(){
		boolean progressive = true;
		long magics[] = new long[64];
		int max = 500000;
		for (int sq = 0; sq<64; sq++){
			if (sq%8==0) System.out.println();
			System.out.print(". ");
			int i = -1;
			while (true){
				i++;
				if (doFindMagic(progressive, rng.nextLong(), sq, magics)) break;
				if (doFindMagic(progressive, ~(rng.nextLong() & rng.nextLong()), sq, magics)) break;
				if (doFindMagic(progressive, ~(rng.nextLong() & rng.nextLong() & rng.nextLong()), sq, magics)) break;
				if (i==max) {
					break;
				}
			}
		}
		System.out.println();
		System.out.print("public static long bishopMagic[] = {");
		for (int sq = 0; sq<64; sq++){
			if (sq%4==0){
				System.out.println();
				System.out.print("   ");
			}
			System.out.print("0x" + Long.toHexString(magics[sq])+"L,");
			if (!progressive && magics[sq]!=0){
				// we can modify the magic (it will be consistent with the state of the bishop move table)
				BB.bishopMagic[sq]=magics[sq];
			} else {
				// return bishop move table to old state!
				int shift = BB.bishopShift[sq];
				int range = 1 << (64-shift);
				boolean resultSet[] = new boolean[range];
				long resultLng[] = new long[range];
				BB.fillBishopBitboards(0L, BB.bishopMask[sq], BB.bishopMagic[sq], shift, sq, resultSet, resultLng);
			}
		}
		System.out.println("};");
	}

	@Test
	public void testSliderMoves(){
		for (int j=0; j<64;j++){
			for (int i=0; i<1000;i++){
				long occupied = rng.nextLong();
				long ctrlBishop = BB.bishopAttacks(1L << j, occupied);
				long actualBishop = BB.getBishopMoves(j, occupied);//BB.bishop[j][result(occupied & BB.bishopMask[j],BB.bishopMagic[j],BB.bishopShift[j])];
				if (ctrlBishop!=actualBishop){
					BB.display(occupied, actualBishop);
				}
				assertEquals(ctrlBishop, actualBishop);
				long ctrlRook = BB.rookAttacks(1L << j, occupied);
				long actualRook = BB.getRookMoves(j, occupied);//BB.bishop[j][result(occupied & BB.bishopMask[j],BB.bishopMagic[j],BB.bishopShift[j])];
				if (ctrlRook!=actualRook){
					System.out.println(j);
					BB.display(occupied, actualRook);
					System.out.println(BB.applyRankMagic(occupied, j)&63);
					BB.display(ctrlRook, actualRook);
				}
				assertEquals(ctrlRook, actualRook);
			}
		}
	}

	@Test
	public void testSortMagic(){
		long inner = ~(BB.file[0] | BB.file[7] | BB.rank[0] | BB.rank[7]);
		for (int i=0; i<64;i++){
			long mask = BB.bishopAttacks(1L << i, (1L << i)) & inner;
			partSortMagic(BB.bishopShift[i], BB.bishopMagic[i], i, mask);
		}
	}

	private void partSortMagic(int shift, long magic, int sq, long mask){
		controlCount = 0;


		int range = 1 << (64-shift);
		boolean resultSet[] = new boolean[range];
		long resultLng[] = new long[range];
		for (int i = 0; i<range; i++){
			resultSet[i] = false;
		}

		fillArrays(0L, mask, resultSet, resultLng, magic, shift, sq);
		assertEquals(controlCount, 1L << Long.bitCount(mask));
		int unused = 0;
		for (int i = 0; i<range; i++){
			if (!resultSet[i]){
				unused++;
			}
		}
//		System.out.println("unused indices: " + unused + " range: " + range + " count: " + controlCount + " mask: " + Long.bitCount(mask));

	}

	private void fillArrays(long done, long todo, boolean resultSet[], long resultLng[], long magic, int shift, int sq){
		if (todo==0){
			long attacks = BB.bishopAttacks(1L << sq, (todo | done));
			controlCount++;
			int result = result(todo | done, magic, shift);
			if (resultSet[result]){
				if (resultLng[result] != attacks){
					BB.display(resultLng[result],attacks);
					System.out.println(result);
					BB.display(todo|done);
				}
				assertEquals(resultLng[result], attacks);
			} else {
				resultLng[result] = attacks;
				resultSet[result] = true;
			}
		} else {
			long twiddleBit = todo & ( - todo);
			fillArrays(done|twiddleBit, todo ^ twiddleBit, resultSet, resultLng, magic, shift, sq);
			fillArrays(done, todo ^ twiddleBit, resultSet, resultLng, magic, shift, sq);
		}
	}

	private int result(long occ, long magic, int shift){
        occ      *= magic;
	    occ     >>>= shift;
		return (int)(occ);
	}

}
