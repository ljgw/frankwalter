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

/**
 * Enumeration of Squares on the 8x8 chess board, with relevant helper functions.
 */
public enum Square {
    A1,B1,C1,D1,E1,F1,G1,H1,
    A2,B2,C2,D2,E2,F2,G2,H2,
    A3,B3,C3,D3,E3,F3,G3,H3,
    A4,B4,C4,D4,E4,F4,G4,H4,
    A5,B5,C5,D5,E5,F5,G5,H5,
    A6,B6,C6,D6,E6,F6,G6,H6,
    A7,B7,C7,D7,E7,F7,G7,H7,
    A8,B8,C8,D8,E8,F8,G8,H8;

    public static final int[] IN_FRONT = {8, -8};

    @Override
    public String toString(){
        return name().toLowerCase();
    }

    /**
     * returns the name of the square associated with the number the square is represented by in the board representation.
     * @param i the number of the square
     * @return the Square
     */
    public static Square byNumber(int i){
        return values()[i];
    }

    /**
     *
     * @param square the number of the square
     * @return true iff the square is white i.e. an even number (like E1)
     */
    public static boolean isWhite(int square) {
        return square%2==0;
    }

    /**
     *
     * @param square the number of the square
     * @return 0 (WHITE) if the square is even (i.e. white like E1) and 1 (BLACK) if the square is odd
     */
    public static int color(int square) {
        return square%2;
    }

    /**
     * returns the square relative for the side to move (so black has the rank inverted)
     * @param square the number of the square
     * @param sideToMove the side to move (0 = white, 1 = black)
     * @return the square relative for the side to move - black has the rank inverted
     */
    public static int relative(int square, int sideToMove) {
        return square^(sideToMove*56);
    }
}
