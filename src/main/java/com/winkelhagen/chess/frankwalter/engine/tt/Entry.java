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

import com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl;

/**
 * Transposition Table Entry
 * 
 * @author Lau
 *
 */
public class Entry {
    public static final byte UNDEFINED = 0;
    /**
     * EXACT entries indicate an exact score: the position scores exactly entry.score
     */
    public static final byte EXACT = 1;
    /**
     * FAIL_LOW entries indicate an upper bound: the position cannot score higher than entry.score
     */
    public static final byte FAIL_LOW = 2;
    /**
     * FAIL_HIGH entries indicate a lower bound: the position cannot score lower than entry.score
     */
    public static final byte FAIL_HIGH = 3;

    public long hashKey;
    public short score;
    public short depth;
    public int move;


    public static final short todepth(short depth2, byte type2) {
        return (short) (depth2 << 2 | (type2 & 0xff));
    } //sonar says the 0xff is necessary.

    public final long asLong() {
        return ((long) move << 32) | (score << 16) & 0xFFFFFFFFL | depth;// & 0xFF.. is needed for negative values.
    }

    public static final int _move(long entry) {
        return (int) (entry >> 32);
    }

    public static final short _score(long entry, int depth) {
        short score = (short) (entry >> 16);
        if (score < ScoutEngineImpl.MATE_TT){
            return (short) (score+depth);
        } else if (score > -ScoutEngineImpl.MATE_TT){
            return (short) (score-depth);
        } else {
            return score;
        }
    }

    public static final short _depth(long entry) {
        return (short) (((short) entry) >> 2);
    }

    public static final short _depth(short entry) {
        return (short) (entry >> 2);
    } //todo: is this one needed anyway?

    public static final byte _type(long entry) {
        return (byte) (entry & 3);
    }

    public static final long toLong(short score, short depth, int move, byte type) {
        return ((long) move << 32) | (score << 16) & 0xFFFFFFFFL | (depth << 2) | type;// & 0xFF.. is needed for negative values.
    }

    public static short correctMatePut(int score, int depth) {
        if (score < ScoutEngineImpl.MATE_TT){
            return (short) (score-depth);
        } else if (score > -ScoutEngineImpl.MATE_TT){
            return (short) (score+depth);
        } else {
            return (short) score;
        }
    }
}
