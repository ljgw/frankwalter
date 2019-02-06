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
package com.winkelhagen.chess.frankwalter.engine.opening;

import java.util.Random;

public interface Book {

    /**
     * set a random number generator (maybe one that has a specific seed for debugging)
     * 
     * @param rnd
     *            a random number generator
     */
    void setRnd(Random rnd);

    /**
     * unload the book from memory (at the mercy of the garbage collection)
     */
    void unloadBook();

    /**
     * probe the book for a move
     * 
     * @param hashKey
     *            the hashKey of the position
     * @return a basic move (int)
     */
    int probeBook(long hashKey);

    /**
     * load a book into memory
     * 
     * @param bookName
     *            the name of the book (minus .dat)
     * @return true if success
     */
    boolean loadBook(String bookName);

    /**
     * returns true of the move is in the book
     * @param move the move
     * @param hashKey the hashKey of the position
     * @return true if the move is in the book.
     */
    boolean isBookMove(int move, long hashKey);
}