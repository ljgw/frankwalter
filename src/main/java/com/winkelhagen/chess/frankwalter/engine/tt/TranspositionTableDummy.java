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

public class TranspositionTableDummy implements TranspositionTable {

    @Override
    public long getEntry(long hashKey) {
        // Dummy
        return 0;
    }

    @Override
    public void setEntry(long hashKey, int score, short selDepth, int move, byte type, int depth) {
        // Dummy
    }

    @Override
    public void free() {
        // Dummy
    }

    @Override
    public void clear() {
        // Dummy
    }
}
