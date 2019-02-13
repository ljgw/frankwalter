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
 * Of the three implementations of the Transposition table, this one seems to work the best in practice: we see a much
 * higher nodecount in short games.
 * 
 * @author Laurens
 *
 */
public class TranspositionTableImpl implements TranspositionTable {
    private Entry[] table;

    private int magnitude; // bits of the key
    private int mask;

    public TranspositionTableImpl(int magnitude) {
        this.magnitude = magnitude;
        this.mask = (1 << magnitude) - 1;
        initTable();
    }

    private void initTable() {
        int size = 1 << magnitude;
        table = new Entry[size];
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable#setEntry(long, short, short, int, byte)
     */
    @Override
    public void setEntry(long hashKey, int score, short selDepth, int move, byte type, int depth) {
        short todepth = Entry.todepth(selDepth, type);
        int key = (int) hashKey & mask;
        Entry entry = table[key];
        if (entry == null) {
            entry = new Entry();
            table[key] = entry;
        } else {
            if (entry.hashKey == hashKey && Entry._depth(entry.depth) > selDepth && type != Entry.EXACT) {
                // If this is the same position AND it is searched deeper then do not overwrite.
                // Unless the new entry is exact: note that then the old entry would not be exact OR would be less deep.
                return;
            }
        }
        entry.hashKey = hashKey;
        entry.depth = todepth;
        entry.score = Entry.correctMateScore(score, todepth);
        entry.move = move;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable#getEntry(long)
     */
    @Override
    public long getEntry(long hashKey) {
        int key = (int) hashKey & mask;
        Entry entry = table[key];
        if (entry != null && entry.hashKey == hashKey)
            return entry.asLong();
        return 0;
    }

    @Override
    public void free() {
        table = null;
    }

    @Override
    public void clear() {
        Arrays.fill(table, null);
    }

    @Override
    public void increaseAge() {
        //no-op
    }

}
