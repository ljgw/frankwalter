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

import java.util.Random;

public class ZobristHash {

    private static final long HASH_OTHERSIDE = 1L;

    private static final Random rng = new MersenneTwister(2071978);

    private static long[][][] zobrist = new long[2][7][64]; // [colors][pieces][squares]
    private static long[] zobristspecial = new long[64]; // [epsquares and castlesquares]

    private long hash;

    static {
        for (int square = 0; square < 64; square++) {
            for (int piece = 0; piece < 7; piece++) {
                if (piece == 0) {
                    zobrist[0][piece][square] = rng.nextLong();
                    zobrist[1][piece][square] = zobrist[0][piece][square];
                } else {
                    zobrist[0][piece][square] = rng.nextLong();
                    zobrist[1][piece][square] = rng.nextLong();
                }
            }
            zobristspecial[square] = rng.nextLong();
        }
    }

    public ZobristHash() {
    }

    public void reset() {
        hash = 0L;
    }

    public boolean equalsHash(long otherHash) {
        return hash == otherHash;
    }

    public long getHash() {
        return hash;
    }

    public void setHash(long hash) {
        this.hash = hash;
    }

    public void toggleSpecial(int specialID) {
        hash ^= zobristspecial[specialID];
    }

    public void toggleSideToMove() {
        hash ^= HASH_OTHERSIDE;
    }

    public void togglePiece(int color, int type, int square) {
        hash ^= zobrist[color][type][square];
    }

    public void doMove(int sideToMove, int movingPiece, int fromSquare, int toSquare, int capturedPiece) {
        togglePiece(sideToMove, movingPiece, fromSquare);
        togglePiece(sideToMove, Constants.EMPTY, fromSquare);
        togglePiece(sideToMove ^ 1, capturedPiece, toSquare);
        togglePiece(sideToMove, movingPiece, toSquare);
    }

    public void completePromotion(int sideToMove, int promotedPiece, int toSquare) {
        togglePiece(sideToMove, promotedPiece, toSquare);
        togglePiece(sideToMove, Constants.PAWN, toSquare);
    }

    public void completeCastle(int sideToMove, int rookToSquare, int rookFromSquare) {
        togglePiece(sideToMove, Constants.ROOK, rookToSquare);
        togglePiece(sideToMove, Constants.EMPTY, rookToSquare);
        togglePiece(sideToMove, Constants.ROOK, rookFromSquare);
        togglePiece(sideToMove, Constants.EMPTY, rookFromSquare);
    }

    public void completeEnPassant(int sideToMove, int epSquare) {
        togglePiece(sideToMove ^ 1, Constants.PAWN, epSquare);
        togglePiece(sideToMove ^ 1, Constants.EMPTY, epSquare);        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZobristHash that = (ZobristHash) o;

        return hash == that.hash;
    }

    @Override
    public int hashCode() {
        return (int) (hash ^ (hash >>> 32));
    }
}
