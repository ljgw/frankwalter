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
import com.winkelhagen.chess.frankwalter.util.Square;
import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;

import java.util.*;

/**
 * very simple piece-square based evaluator
 *
 * @author Lau
 */
public class Evaluator {

    public static final int KILLERSCORE = 800;
    public static final int CAPTURESCORE = 900;
    public static final int HASHSCORE = 1000;
//    private static final int[] pieceValueKingSafety = { 0, -20, -10, -8, -7, -0, -3 };
    private static final int MOBILITY_FACTOR = 5; //used to be 4.
    private static final int PCSQ_PRECISION = 4;
    public static final int[][] MOVE_PCSQ = {
            {
                    0,2,4,6,6,4,2,0,
                    2,4,6,8,8,6,4,2,
                    4,6,8,10,10,8,6,4,
                    6,8,10,12,12,10,8,6,
                    6,8,10,12,12,10,8,6,
                    4,6,8,10,10,8,6,4,
                    2,4,6,8,8,6,4,2,
                    0,2,4,6,6,4,2,0
            },
            {
                    4,0,0,6,6,0,0,4,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    8,8,8,8,8,8,8,8,
                    6,6,6,6,6,6,6,6
            },
            {
                    0,2,4,6,6,4,2,0,
                    2,4,6,8,8,6,4,2,
                    4,6,8,10,10,8,6,4,
                    6,8,10,12,12,10,8,6,
                    6,8,10,12,12,10,8,6,
                    4,6,8,10,10,8,6,4,
                    2,4,6,8,8,6,4,2,
                    0,2,4,6,6,4,2,0
            },
            {
                    0,2,4,6,6,4,2,0,
                    2,4,6,8,8,6,4,2,
                    4,6,8,10,10,8,6,4,
                    6,8,10,12,12,10,8,6,
                    6,8,10,12,12,10,8,6,
                    4,6,8,10,10,8,6,4,
                    2,4,6,8,8,6,4,2,
                    0,2,4,6,6,4,2,0,
            },
            {
                    0,8,0,6,6,0,8,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0
            },
            {
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,0,0,0,0,0,0,
                    0,0,4,6,6,4,0,0,
                    0,0,0,6,6,0,0,0,
                    2,2,2,2,2,2,2,2,
                    4,4,4,4,4,4,4,4,
                    10,10,10,10,10,10,10,10
            }
    };

    private static final int PAWN_PHASE = 0;
    private static final int KNIGHT_PHASE = 1;
    private static final int BISHOP_PHASE = 1;
    private static final int ROOK_PHASE = 2;
    private static final int QUEEN_PHASE = 4;
    private static final int TOTAL_PHASE = PAWN_PHASE *16 + KNIGHT_PHASE *4 + BISHOP_PHASE *4 + ROOK_PHASE *4 + QUEEN_PHASE *2;
    private static final int CONTEMPT = 0;


    private static int[] _knightPSQ = {292, 344, 332, 340, 340, 332, 344, 292, 340, 320, 360, 372, 372, 360, 320, 340, 340, 368, 380, 384, 384, 380, 368, 340, 344, 376, 380, 380, 380, 380, 376, 344, 372, 376, 400, 400, 400, 400, 376, 372, 340, 428, 396, 432, 432, 396, 428, 340, 272, 312, 396, 356, 356, 396, 312, 272, 168, 268, 240, 364, 364, 240, 268, 168};
    private static int[] _knightEndgamePSQ = {256, 248, 280, 284, 284, 280, 248, 256, 256, 280, 288, 292, 292, 288, 280, 256, 272, 288, 296, 312, 312, 296, 288, 272, 280, 296, 320, 324, 324, 320, 296, 280, 276, 304, 316, 324, 324, 316, 304, 276, 264, 272, 308, 296, 296, 308, 272, 264, 268, 288, 272, 300, 300, 272, 288, 268, 244, 252, 288, 264, 264, 288, 252, 244};
    private static int[] _bishopPSQ = {320, 340, 344, 340, 340, 344, 340, 320, 356, 372, 360, 352, 352, 360, 372, 356, 348, 356, 364, 348, 348, 364, 356, 348, 336, 344, 340, 360, 360, 340, 344, 336, 340, 332, 348, 368, 368, 348, 332, 340, 324, 368, 364, 360, 360, 364, 368, 324, 284, 340, 316, 340, 340, 316, 340, 284, 316, 316, 268, 264, 264, 268, 316, 316};
    private static int[] _bishopEndgamePSQ = {272, 280, 272, 280, 280, 272, 280, 272, 260, 264, 268, 276, 276, 268, 264, 260, 276, 276, 272, 284, 284, 272, 276, 276, 280, 276, 280, 276, 276, 280, 276, 280, 284, 284, 280, 272, 272, 280, 284, 284, 288, 272, 272, 264, 264, 272, 272, 288, 288, 276, 284, 268, 268, 284, 276, 288, 268, 272, 280, 288, 288, 280, 272, 268};
    private static int[] _rookPSQ = {456, 452, 472, 480, 480, 472, 452, 456, 420, 452, 452, 456, 456, 452, 452, 420, 428, 440, 448, 448, 448, 448, 440, 428, 424, 444, 436, 448, 448, 436, 444, 424, 428, 440, 456, 452, 452, 456, 440, 428, 448, 484, 464, 448, 448, 464, 484, 448, 476, 476, 508, 516, 516, 508, 476, 476, 444, 476, 428, 480, 480, 428, 476, 444};
    private static int[] _rookEndgamePSQ = {476, 484, 476, 468, 468, 476, 484, 476, 484, 472, 472, 472, 472, 472, 472, 484, 480, 480, 472, 468, 468, 472, 480, 480, 488, 484, 484, 472, 472, 484, 484, 488, 496, 484, 488, 480, 480, 488, 484, 496, 488, 480, 484, 484, 484, 484, 480, 488, 492, 488, 480, 472, 472, 480, 488, 492, 500, 488, 500, 484, 484, 500, 488, 500};
    private static int[] _queenPSQ = {944, 948, 944, 960, 960, 944, 948, 944, 928, 936, 952, 940, 940, 952, 936, 928, 924, 936, 916, 916, 916, 916, 936, 924, 924, 904, 904, 892, 892, 904, 904, 924, 920, 888, 900, 872, 872, 900, 888, 920, 952, 936, 900, 904, 904, 900, 936, 952, 920, 868, 912, 884, 884, 912, 868, 920, 916, 912, 928, 956, 956, 928, 912, 916};
    private static int[] _queenEndgamePSQ = {920, 908, 916, 912, 912, 916, 908, 920, 936, 916, 912, 932, 932, 912, 916, 936, 968, 936, 968, 952, 952, 968, 936, 968, 964, 996, 976, 988, 988, 976, 996, 964, 988, 1012, 984, 1008, 1008, 984, 1012, 988, 952, 960, 1000, 1012, 1012, 1000, 960, 952, 964, 992, 992, 1032, 1032, 992, 992, 964, 980, 992, 996, 980, 980, 996, 992, 980};
    private static int[] _kingPSQ = {84, 136, 112, 56, 120, 84, 156, 132, 136, 120, 64, 44, 44, 64, 120, 136, 76, 88, 56, 28, 28, 56, 88, 76, 24, 64, 28, 4, 4, 28, 64, 24, 48, 84, 80, 56, 56, 80, 84, 48, 128, 180, 196, 116, 116, 196, 180, 128, 80, 96, 112, 168, 168, 112, 96, 80, 64, 192, 140, 108, 108, 140, 192, 64};
    private static int[] _kingEndgamePSQ = {16, 40, 64, 56, 56, 64, 40, 16, 52, 76, 96, 104, 104, 96, 76, 52, 72, 88, 104, 116, 116, 104, 88, 72, 76, 92, 116, 124, 124, 116, 92, 76, 84, 112, 116, 116, 116, 116, 112, 84, 84, 104, 104, 96, 96, 104, 104, 84, 88, 100, 108, 88, 88, 108, 100, 88, 36, 48, 72, 60, 60, 72, 48, 36};
    private static int[] _pawnPSQ = {0, 0, 0, 0, 0, 0, 0, 0, 60, 84, 80, 72, 72, 80, 84, 60, 68, 80, 84, 84, 84, 84, 80, 68, 52, 72, 80, 96, 96, 80, 72, 52, 60, 84, 84, 108, 108, 84, 84, 60, 60, 68, 100, 68, 68, 100, 68, 60, -104, -44, -68, -20, -20, -68, -44, -104, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int[] _pawnEndgamePSQ = {0, 0, 0, 0, 0, 0, 0, 0, 88, 84, 92, 88, 88, 92, 84, 88, 84, 80, 84, 84, 84, 84, 80, 84, 96, 88, 84, 76, 76, 84, 88, 96, 112, 100, 92, 72, 72, 92, 100, 112, 140, 136, 104, 84, 84, 104, 136, 140, 232, 200, 168, 148, 148, 168, 200, 232, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int[] _pawnBonus = {50, 63, 76, 84, 92, 101, 112, 130, 161};
    private static int[] _pawnRace = {3, 4, 18, 39, 86, 164};

    public static int eval(Board board, int alpha, int beta) {
        //Lazy Eval block
        {
            int simpleScore = board.getMaterialScore(Constants.WHITE) - board.getMaterialScore(Constants.BLACK);
            if (board.getSideToMove() == Constants.BLACK)
                simpleScore = 0 - simpleScore;
            if (simpleScore >= beta + Constants.COMPLEX_EVAL_MARGIN) {
                return simpleScore - Constants.COMPLEX_EVAL_MARGIN;
            }
            if (simpleScore <= alpha - Constants.COMPLEX_EVAL_MARGIN) {
                return simpleScore + Constants.COMPLEX_EVAL_MARGIN;
            }
        }

        // this could cause the same position to have multiple values, which might disturb the Transposition Table.
        if (board.getQuiet50() > 99) {
            if (board.getSideToMove() == Constants.BLACK) {
                return getContemptScore();
            } else {
                return -getContemptScore();
            }
        }

        // evaluate both sides - the scores for the first side to evaluate are subtracted from the second side to evaulate.
        return eval(board, true, board.getSideToMove()^1, 0, 0, 0, TOTAL_PHASE);
    }

    private static int eval(Board board, boolean negateThisSide, int sideToScore, int score, int scoreMid, int scoreEnd, int phase){
        long[] attacking = board.getAttacking();
        long pieces = board.getPieces()[sideToScore][Constants.ALL];
        int bishops = Long.bitCount(board.getPieces()[sideToScore][Constants.BISHOP]);

        if (bishops==2) {
            score +=25;
        }


        // Penalize pins
        score -= Long.bitCount(board.getPins(sideToScore)) * 4;

        while (pieces != 0) {
            int square = BB.lsb(pieces);
            pieces &= pieces - 1;
            switch (board.getSquares()[square]) {
            case Constants.KNIGHT:
                scoreMid += _knightPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _knightEndgamePSQ[Square.relative(square, sideToScore)];
                phase -= KNIGHT_PHASE;
                break;
            case Constants.BISHOP:
                scoreMid += _bishopPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _bishopEndgamePSQ[Square.relative(square, sideToScore)];
                score += Long.bitCount(attacking[square]) * MOBILITY_FACTOR;
                phase -= BISHOP_PHASE;
                break;
            case Constants.ROOK:
                scoreMid += _rookPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _rookEndgamePSQ[Square.relative(square, sideToScore)];
                score += Long.bitCount(attacking[square]) * MOBILITY_FACTOR;
                if ((BB.file[BB.fileOf(square)] & board.getPieces()[sideToScore][Constants.PAWN]) == 0) {
                    score += 12;
                    if ((BB.file[BB.fileOf(square)] & board.getPieces()[sideToScore^1][Constants.PAWN]) == 0) {
                        score += 12;
                    }
                }
                phase -= ROOK_PHASE;
                break;
            case Constants.QUEEN:
                scoreMid += _queenPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _queenEndgamePSQ[Square.relative(square, sideToScore)];
                score += Long.bitCount(attacking[square]) * MOBILITY_FACTOR;
                phase -= QUEEN_PHASE;
                break;
            case Constants.KING:
                scoreMid += _kingPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _kingEndgamePSQ[Square.relative(square, sideToScore)];
                break;
            case Constants.PAWN:
                int rank = BB.rankOf(square)^(sideToScore*7);
                int file = BB.fileOf(square);
                if ((BB.thickFileInFront[sideToScore][square]
                        & board.getPieces()[sideToScore^1][Constants.PAWN]) == 0) {
                    score += _pawnRace[rank - 1];
                }
                if ((BB.closeFiles[file] & board.getPieces()[sideToScore][Constants.PAWN]) == 0)
                    score -= 12;
                if ((BB.fileInFront[sideToScore][square] & board.getPieces()[sideToScore][Constants.PAWN]) != 0)
                    score -= 28;
                scoreMid += _pawnPSQ[Square.relative(square, sideToScore)];
                scoreEnd += _pawnEndgamePSQ[Square.relative(square, sideToScore)];
                phase -= PAWN_PHASE;
                break;
            default:
            }
        }

        // PAWN BONUSSES
        int pawns = Long.bitCount(board.getPieces()[sideToScore][Constants.PAWN]);
        score = score + _pawnBonus[pawns];

        if (negateThisSide){
            return eval(board, false, sideToScore^1, -score, -scoreMid, -scoreEnd, phase);
        } else {
            score += calculatePcsqForPhase(scoreMid, scoreEnd, phase);
            return score;
        }
    }

    private static int calculatePcsqForPhase(int pscq_mid, int pscq_end, int phase) {
        int phaseMod = (phase * 256 + (TOTAL_PHASE / 2)) / TOTAL_PHASE;
        return ((pscq_mid * (256 - phaseMod)) + (pscq_end * phaseMod)) / 256;
    }

    /**
     * score for a draw, based on contempt. Other formula's than return 0 are untested.
     * 
     * @return the contempt constant
     */
    public static int getContemptScore() {
        return CONTEMPT;
    }

}
