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
package com.winkelhagen.chess.frankwalter.engine.evaluator;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;

public class Seer {

    private Seer(){
        //empty
    }

    private static final int[] SEE_VALUE = { 0, 9, 5, 3, 3, 100, 1 };

    //todo: maybe somehow add a bonus to pawn promotions, iff not captured. (try to do those last)
    public static int see(int move, Board board) {
        int battleGround = MV.getToSquare(move);
        int capture = board.getSquares()[battleGround];
        int fromSquare = MV.getFromSquare(move);
        int mover = board.getSquares()[fromSquare];
        int stm = board.getSideToMove();
        long defenders = board.getAttacked()[battleGround];
        long currentlyOccupied = board.getOccupied() ^ BB.single(fromSquare);
        if (mover != Constants.KNIGHT) {
            defenders = addUncoveredDefenders(battleGround, currentlyOccupied, defenders, board.getPieces());
        }
        return seeRecursive(battleGround, stm ^ 1, mover, capture, currentlyOccupied, defenders, board.getSquares(), board.getPieces());
    }

    /*
    calculate the SEE score that reflects the 'stm' capturing 'capture' with 'mover'
    Use recursion to see how the score might be affected when 'mover' becomes the new 'capture'
     */
    private static int seeRecursive(int battleGround, int stm, int mover, int capture, long currentlyOccupied, long defenders, int[] squares, long[][] pieces) {
        //determine the remaining defenders
        long remainingDefenders = defenders & pieces[stm][Constants.ALL] & currentlyOccupied;
        if (remainingDefenders == 0 || capture == Constants.KING) {
            //if there is no recapture then return the value of the captured piece
            return SEE_VALUE[capture];
        } else {
            int fromSquare = getLowestDefender(remainingDefenders, squares);
            currentlyOccupied ^= BB.single(fromSquare);
            if (mover != Constants.KNIGHT) {
                defenders = addUncoveredDefenders(battleGround, currentlyOccupied, defenders, pieces);
            }
            int score = -seeRecursive(battleGround, stm ^ 1, squares[fromSquare], mover, currentlyOccupied, defenders, squares, pieces);
            if (score > 0) {
                return SEE_VALUE[capture];
            } else {
                return SEE_VALUE[capture] + score;
            }
        }
    }

    private static int getLowestDefender(long remainingDefenders, int[] squares) {
        int lowestDefendingSquare = 0;
        int lowestDefenderValue = 10000;
        while (remainingDefenders != 0) {
            int defender = BB.lsb(remainingDefenders);
            int value = SEE_VALUE[squares[defender]];
            remainingDefenders &= remainingDefenders - 1;
            if (value < lowestDefenderValue) {
                lowestDefendingSquare = defender;
                lowestDefenderValue = value;
            }
        }
        return lowestDefendingSquare;
    }

    private static long addUncoveredDefenders(int battleGround, long currentlyOccupied, long defenders, long[][] pieces) {
        long rookSliders = pieces[Constants.WHITE][Constants.ROOK] | pieces[Constants.WHITE][Constants.QUEEN] |
                pieces[Constants.BLACK][Constants.ROOK] | pieces[Constants.BLACK][Constants.QUEEN];
        if (rookSliders!=0) {
            defenders |= BB.getRookMoves(battleGround, currentlyOccupied) & rookSliders;
        }
        long bishopSliders = pieces[Constants.WHITE][Constants.BISHOP] | pieces[Constants.WHITE][Constants.QUEEN] |
                pieces[Constants.BLACK][Constants.BISHOP] | pieces[Constants.BLACK][Constants.QUEEN];
        if (bishopSliders!=0) {
            defenders |= BB.getBishopMoves(battleGround, currentlyOccupied) & bishopSliders;
        }
        return defenders;
    }
}
