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

import java.util.Arrays;

/**
 * Of the three implementations of the Transposition table, this one seems to work the best in practice: we see a much higher nodecount in short games.
 * @author Laurens
 *
 */
public class TranspositionTableQuadArrayImpl implements TranspositionTable {
	private long[] table = null;

	private int magnitude; //bits of the key
	private long mask;
	private long inverseMask;
	private int currentAge;

	public TranspositionTableQuadArrayImpl(int magnitude){
		this.magnitude = magnitude;
		this.mask = (1 << (magnitude-2)) -1L;
		this.inverseMask = ~mask;
		initTable();
	}
	
	private void initTable(){
		int size = 1 << magnitude;
		table = new long[size*2];
		currentAge = 0;
	}

	@Override
	public void increaseAge(){
		currentAge = (currentAge+1)&(int)mask;
	}
	
	/* (non-Javadoc)
	 * @see com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable#setEntry(long, short, short, int, byte)
	 */
    @Override
	public void setEntry(long hashKey, int score, short selDepth, int move, byte type, int depth){
    	int maskedKey = (int)(hashKey & mask);
		int key = maskedKey<<2;
		int replaceIndex = 0;
		int replaceDepth = Integer.MAX_VALUE;
		for (int i=0; i<4;i++) {
			long entryHash = table[(key+i) * 2];
			if (entryHash==0){
				replaceIndex = i;
				break;
			}
			long entry = table[(key+i)*2+1];
			int entryDepth = Entry._depth(entry);
			if ((entryHash&inverseMask) == (hashKey&inverseMask)) {
				//this is the one
				if (entryDepth>selDepth && type != Entry.EXACT){
					return;
				}
				replaceIndex = i;
				break;
			}

			int entryAge = (int)(entryHash&mask);
			//entries from an older search can be overwritten anyway
			if (entryAge!=currentAge){
				replaceIndex = i;
				break;
				//replace least deep: todo: try to de-prioritize replacing EXACT entries
			} else if (entryDepth<replaceDepth){
				replaceIndex = i;
				replaceDepth = entryDepth;
			}
		}
		table[(key+replaceIndex)*2] = (hashKey^(long)maskedKey)|(long)currentAge;
		table[(key+replaceIndex)*2+1] = Entry.toLong(Entry.correctMateScore(score, depth), selDepth, move, type);
	}
	
	/* (non-Javadoc)
	 * @see com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable#getEntry(long)
	 */
    @Override
	public long getEntry(long hashKey){
		int key = (int)(hashKey & mask)<<2;
		for (int i=0; i<4;i++) {
			long entryHash = table[(key+i) * 2];
			if ((entryHash&inverseMask) == (hashKey&inverseMask)) {
				return table[(key+i) * 2 + 1];
			}
		}
		return 0;
	}

	@Override
	public void free() {
		table = null;		
	}

	@Override
	public void clear() {
		currentAge = 0;
		Arrays.fill(table, 0);
	}

}
