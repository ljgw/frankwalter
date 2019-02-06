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
package com.winkelhagen.chess.frankwalter.engine.tb;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import com.winkelhagen.chess.syzygy.SyzygyConstants;

/**
 * converter class to fit the FrankWalter board representation on the SyzygyBridge
 */
public class Syzygy {

    private Syzygy(){}

    /**
     * wrapper for {@link com.winkelhagen.chess.syzygy.SyzygyBridge#isAvailable(int)}
     * @param piecesLeft the number of pieces left on the board
     * @return true iff there is a Syzygy result to be expected, given the number of pieces currently on the board
     */
    public static boolean isAvailable(int piecesLeft){
        return SyzygyBridge.isAvailable(piecesLeft);
    }

    /**
     * probes the Syzygy TableBases for a WinDrawLoss result
     * @param board the FrankWalter board representation
     * @return a WDL result (see {@link #getWDLScore(int, int)})
     */
    public static int probeWDL(Board board){
        long[][] pc = board.getPieces();
        return SyzygyBridge.probeSyzygyWDL(
                pc[Constants.WHITE][Constants.KING]|pc[Constants.WHITE][Constants.QUEEN]|pc[Constants.WHITE][Constants.ROOK]|pc[Constants.WHITE][Constants.BISHOP]|pc[Constants.WHITE][Constants.KNIGHT]|pc[Constants.WHITE][Constants.PAWN],
                pc[Constants.BLACK][Constants.KING]|pc[Constants.BLACK][Constants.QUEEN]|pc[Constants.BLACK][Constants.ROOK]|pc[Constants.BLACK][Constants.BISHOP]|pc[Constants.BLACK][Constants.KNIGHT]|pc[Constants.BLACK][Constants.PAWN],
                pc[Constants.WHITE][Constants.KING]|pc[Constants.BLACK][Constants.KING],
                pc[Constants.WHITE][Constants.QUEEN]|pc[Constants.BLACK][Constants.QUEEN],
                pc[Constants.WHITE][Constants.ROOK]|pc[Constants.BLACK][Constants.ROOK],
                pc[Constants.WHITE][Constants.BISHOP]|pc[Constants.BLACK][Constants.BISHOP],
                pc[Constants.WHITE][Constants.KNIGHT]|pc[Constants.BLACK][Constants.KNIGHT],
                pc[Constants.WHITE][Constants.PAWN]|pc[Constants.BLACK][Constants.PAWN],
                board.getEpSquare()==-1?0:board.getEpSquare(),
                board.getSideToMove()==Constants.WHITE
        );
    }

    /**
     * probes the Syzygy TableBases for a DistanceToZero result.
     * If castling is still allowed, no accurate DTZ can be given
     * @param board the FrankWalter board representation
     * @return a WDL result (see {@link #toXBoardScore(int)} and {@link #toMove(int)})
     */
    public static int probeDTZ(Board board){
        if (board.getCastleMask()!=0){
            return -1;
        }
        long[][] pc = board.getPieces();
        return SyzygyBridge.probeSyzygyDTZ(
                pc[Constants.WHITE][Constants.KING]|pc[Constants.WHITE][Constants.QUEEN]|pc[Constants.WHITE][Constants.ROOK]|pc[Constants.WHITE][Constants.BISHOP]|pc[Constants.WHITE][Constants.KNIGHT]|pc[Constants.WHITE][Constants.PAWN],
                pc[Constants.BLACK][Constants.KING]|pc[Constants.BLACK][Constants.QUEEN]|pc[Constants.BLACK][Constants.ROOK]|pc[Constants.BLACK][Constants.BISHOP]|pc[Constants.BLACK][Constants.KNIGHT]|pc[Constants.BLACK][Constants.PAWN],
                pc[Constants.WHITE][Constants.KING]|pc[Constants.BLACK][Constants.KING],
                pc[Constants.WHITE][Constants.QUEEN]|pc[Constants.BLACK][Constants.QUEEN],
                pc[Constants.WHITE][Constants.ROOK]|pc[Constants.BLACK][Constants.ROOK],
                pc[Constants.WHITE][Constants.BISHOP]|pc[Constants.BLACK][Constants.BISHOP],
                pc[Constants.WHITE][Constants.KNIGHT]|pc[Constants.BLACK][Constants.KNIGHT],
                pc[Constants.WHITE][Constants.PAWN]|pc[Constants.BLACK][Constants.PAWN],
                board.getQuiet50(),
                board.getEpSquare()==-1?0:board.getEpSquare(),
                board.getSideToMove()==Constants.WHITE
        );
    }


    public static int toMove(int result){
        int from = (result & SyzygyConstants.TB_RESULT_FROM_MASK) >> SyzygyConstants.TB_RESULT_FROM_SHIFT;
        int to = (result & SyzygyConstants.TB_RESULT_TO_MASK) >> SyzygyConstants.TB_RESULT_TO_SHIFT;
        int promotes = (result & SyzygyConstants.TB_RESULT_PROMOTES_MASK) >> SyzygyConstants.TB_RESULT_PROMOTES_SHIFT;
        return MV.getMove(from, to, promotes);
    }

    /**
     * returns the score associated to the move in the result (xboard compatible, i.e. (+/-) 28000-full moves to win/lose or 0 for draw.
     *
     * @param result of the DTZ tablebase operation
     * @return the score to be displayed by xboard
     * todo: fix: this returns DTZero, not DTMate.
     */
    public static int toXBoardScore(int result){
        int dtz = (result & SyzygyConstants.TB_RESULT_DTZ_MASK) >> SyzygyConstants.TB_RESULT_DTZ_SHIFT;
        int dtzFull = (dtz+1)/2;
        int wdl = (result & SyzygyConstants.TB_RESULT_WDL_MASK) >> SyzygyConstants.TB_RESULT_WDL_SHIFT;
        switch (wdl){
            case SyzygyConstants.TB_LOSS:
                return -28000 + dtzFull; //LW DTM: -100000 - dtzFull
            case SyzygyConstants.TB_BLESSED_LOSS:
                return 0;
            case SyzygyConstants.TB_DRAW:
                return 0;
            case SyzygyConstants.TB_CURSED_WIN:
                return +28000 - dtzFull; //LW DTM: +100000 + dtzFull
            case SyzygyConstants.TB_WIN:
                return +28000 - dtzFull; //LW DTM: +100000 + dtzFull
            default:
                return 0;
        }
    }

    /**
     * returns the score to use inside the main search, based on the WDL result of a TableBase query and the search depth
     * @param wdl the WinDrawLoss result from the probe
     * @param depth the depth of the current search
     * @return the score associated with this position
     */
    public static int getWDLScore(int wdl, int depth) {
        switch (wdl){
            case SyzygyConstants.TB_LOSS:
                return -28000 + depth;
            case SyzygyConstants.TB_BLESSED_LOSS:
                return -27000 + depth;
            case SyzygyConstants.TB_DRAW:
                return 0;
            case SyzygyConstants.TB_CURSED_WIN:
                return 27000 - depth;
            case SyzygyConstants.TB_WIN:
                return 28000 - depth;
            default:
                return 0;
        }
    }
}
