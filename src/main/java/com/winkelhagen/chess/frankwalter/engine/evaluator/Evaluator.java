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


    private static int[] _knightPSQ;
    private static int[] _knightEndgamePSQ;
    private static int[] _bishopPSQ;
    private static int[] _bishopEndgamePSQ;
    private static int[] _rookPSQ;
    private static int[] _rookEndgamePSQ;
    private static int[] _queenPSQ;
    private static int[] _queenEndgamePSQ;
    private static int[] _kingPSQ;
    private static int[] _kingEndgamePSQ;
    private static int[] _pawnPSQ;
    private static int[] _pawnEndgamePSQ;
    private static int[] _pawnBonus;
    private static int[] _pawnRace;
    private static int _ISOLATED_PAWN_PENALTY;
    private static int _DOUBLED_PAWN_PENALTY;
    private static int _CONNECTED_PAWN_BONUS;
    private static int[] _PAWN_SHELTER_BONUS;


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
        int score = eval(board, true, board.getSideToMove()^1, 0, 0, 0, TOTAL_PHASE);

        //taper down scores near 50move edge
        if (board.getQuiet50() > 79) {
            if (board.getSideToMove() == Constants.BLACK) {
                return calculate50MoveDrawScore(board.getQuiet50()-80, score, getContemptScore());
            } else {
                return calculate50MoveDrawScore(board.getQuiet50()-80, score, -getContemptScore());
            }
        }

        return score;
    }

    private static int calculate50MoveDrawScore(int scale, int score, int comtempt) {
        return (comtempt*scale + (20-scale)*score)/20;
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
                scoreMid += _PAWN_SHELTER_BONUS[Math.min(Long.bitCount(BB.thickFileInFront[sideToScore][square] & board.getPieces()[sideToScore][Constants.PAWN] & BB.rank23[sideToScore]), 4)];
                scoreEnd += _kingEndgamePSQ[Square.relative(square, sideToScore)];
                break;
            case Constants.PAWN:
                int rank = BB.rankOf(square)^(sideToScore*7);
                int file = BB.fileOf(square);
                //passed pawn
                if ((BB.thickFileInFront[sideToScore][square]
                        & board.getPieces()[sideToScore^1][Constants.PAWN]) == 0) {
                    score += _pawnRace[rank - 1];
                }
                //isolated pawn
                if ((BB.closeFiles[file] & board.getPieces()[sideToScore][Constants.PAWN]) == 0) {
                    score -= _ISOLATED_PAWN_PENALTY;
                }
                //double pawn
                if ((BB.fileInFront[sideToScore][square] & board.getPieces()[sideToScore][Constants.PAWN]) != 0) {
                    score -= _DOUBLED_PAWN_PENALTY;
                }
                //connected pawn
                if (((board.getAttacked()[square] | board.getAttacked()[square + Square.IN_FRONT[sideToScore]]) & board.getPieces()[sideToScore][Constants.PAWN]) != 0){
                    score += _CONNECTED_PAWN_BONUS;
                }
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

//tuning

    private static int[] applyToArray(int[] target, int[] weights, int offset){
        int[] result = new int[target.length];
        for (int i=0; i<target.length; i++){
            result[i] = target[i]+weights[i+offset];
        }
        return result;
    }

    private static int[] applyToPcsqArray(int[] target, int[] weights, int offset){
        int[] result = new int[64];
        for (int i=0; i<4; i++){
            for (int j=0; j<8; j++) {
                int k = j*8+i;
                int l = j*8+7-i;
                int m = j*4+i;
                result[k] = (target[k]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[m + offset] * PCSQ_PRECISION;
                result[l] = (target[l]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[m + offset] * PCSQ_PRECISION;
            }
        }
        return result;
    }

    /*
    different because a king on d1 is different to e1.
     */
    private static int[] applyToKingPcsqArray(int[] target, int[] weights, int offset){
        int[] result = new int[64];
        for (int i=0; i<4; i++){
            for (int j=-1; j<8; j++) {
                int k = j*8+i;
                int l = j*8+7-i;
                int m = j*4+i;
                if (j==-1){
                    result[i] = (target[i]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[i + offset] * PCSQ_PRECISION;
                } else if (j==0){
                    result[l] = (target[l]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[m + offset + 4] * PCSQ_PRECISION;
                } else {
                    result[k] = (target[k]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[m + offset + 4] * PCSQ_PRECISION;
                    result[l] = (target[l]/PCSQ_PRECISION)*PCSQ_PRECISION + weights[m + offset + 4] * PCSQ_PRECISION;
                }
            }
        }
        return result;
    }

    public static void applyWeights(int[] weights) {
        _knightPSQ = applyToPcsqArray(knightPSQ, weights, 0);
        _knightEndgamePSQ = applyToPcsqArray(knightEndgamePSQ, weights, 32);
        _bishopPSQ = applyToPcsqArray(bishopPSQ, weights, 64);
        _bishopEndgamePSQ = applyToPcsqArray(bishopEndgamePSQ, weights, 96);
        _rookPSQ = applyToPcsqArray(rookPSQ, weights, 128);
        _rookEndgamePSQ = applyToPcsqArray(rookEndgamePSQ, weights, 160);
        _queenPSQ = applyToPcsqArray(queenPSQ, weights, 192);
        _queenEndgamePSQ = applyToPcsqArray(queenEndgamePSQ, weights, 224);
        _kingPSQ = applyToKingPcsqArray(kingPSQ, weights, 256);
        _kingEndgamePSQ = applyToPcsqArray(kingEndgamePSQ, weights, 292);
        _pawnPSQ = applyToPcsqArray(pawnPSQ, weights, 324);
        _pawnEndgamePSQ = applyToPcsqArray(pawnEndgamePSQ, weights, 356);

        _pawnBonus = applyToArray(pawnBonus, weights, 388);
        _pawnRace = applyToArray(pawnRace, weights, 397);
        _ISOLATED_PAWN_PENALTY = ISOLATED_PAWN_PENALTY + weights[403];
        _DOUBLED_PAWN_PENALTY = DOUBLED_PAWN_PENALTY + weights[404];
        _CONNECTED_PAWN_BONUS = CONNECTED_PAWN_BONUS + weights[405];
        _PAWN_SHELTER_BONUS = applyToArray(PAWN_SHELTER_BONUS, weights, 406);
    }

    private static int[] knightPSQ = {288, 348, 332, 348, 348, 332, 348, 288, 344, 328, 364, 376, 376, 364, 328, 344, 340, 372, 384, 388, 388, 384, 372, 340, 348, 380, 384, 384, 384, 384, 380, 348, 372, 380, 404, 404, 404, 404, 380, 372, 340, 428, 400, 428, 428, 400, 428, 340, 276, 316, 400, 356, 356, 400, 316, 276, 172, 264, 248, 364, 364, 248, 264, 172};
    private static int[] knightEndgamePSQ = {260, 248, 280, 280, 280, 280, 248, 260, 252, 280, 288, 292, 292, 288, 280, 252, 276, 288, 296, 312, 312, 296, 288, 276, 280, 296, 320, 324, 324, 320, 296, 280, 276, 304, 316, 324, 324, 316, 304, 276, 264, 272, 308, 300, 300, 308, 272, 264, 268, 288, 272, 300, 300, 272, 288, 268, 244, 252, 288, 264, 264, 288, 252, 244};
    private static int[] bishopPSQ = {320, 340, 344, 344, 344, 344, 340, 320, 352, 372, 360, 352, 352, 360, 372, 352, 352, 356, 364, 348, 348, 364, 356, 352, 340, 348, 340, 364, 364, 340, 348, 340, 336, 336, 352, 372, 372, 352, 336, 336, 324, 364, 368, 352, 352, 368, 364, 324, 288, 340, 316, 332, 332, 316, 340, 288, 320, 316, 268, 256, 256, 268, 316, 320};
    private static int[] bishopEndgamePSQ = {272, 280, 272, 280, 280, 272, 280, 272, 264, 264, 272, 276, 276, 272, 264, 264, 276, 276, 276, 284, 284, 276, 276, 276, 280, 276, 284, 276, 276, 284, 276, 280, 284, 284, 280, 272, 272, 280, 284, 284, 288, 272, 272, 268, 268, 272, 272, 288, 284, 276, 284, 272, 272, 284, 276, 284, 272, 272, 280, 288, 288, 280, 272, 272};
    private static int[] rookPSQ = {456, 452, 468, 476, 476, 468, 452, 456, 420, 452, 456, 460, 460, 456, 452, 420, 428, 444, 448, 444, 444, 448, 444, 428, 428, 448, 440, 452, 452, 440, 448, 428, 428, 440, 456, 452, 452, 456, 440, 428, 448, 476, 464, 448, 448, 464, 476, 448, 476, 476, 508, 512, 512, 508, 476, 476, 448, 480, 432, 476, 476, 432, 480, 448};
    private static int[] rookEndgamePSQ = {476, 484, 480, 472, 472, 480, 484, 476, 488, 472, 472, 472, 472, 472, 472, 488, 480, 480, 472, 472, 472, 472, 480, 480, 488, 484, 484, 472, 472, 484, 484, 488, 496, 484, 488, 480, 480, 488, 484, 496, 488, 484, 484, 484, 484, 484, 484, 488, 492, 488, 480, 472, 472, 480, 488, 492, 500, 488, 500, 484, 484, 500, 488, 500};
    private static int[] queenPSQ = {940, 944, 944, 960, 960, 944, 944, 940, 928, 936, 952, 940, 940, 952, 936, 928, 924, 936, 916, 916, 916, 916, 936, 924, 924, 904, 904, 892, 892, 904, 904, 924, 916, 888, 900, 872, 872, 900, 888, 916, 952, 932, 896, 900, 900, 896, 932, 952, 920, 868, 908, 884, 884, 908, 868, 920, 920, 912, 924, 956, 956, 924, 912, 920};
    private static int[] queenEndgamePSQ = {928, 916, 916, 916, 916, 916, 916, 928, 940, 920, 916, 936, 936, 916, 920, 940, 968, 936, 972, 956, 956, 972, 936, 968, 972, 1000, 976, 988, 988, 976, 1000, 972, 996, 1012, 992, 1012, 1012, 992, 1012, 996, 952, 964, 1004, 1020, 1020, 1004, 964, 952, 964, 996, 1000, 1032, 1032, 1000, 996, 964, 980, 996, 1000, 980, 980, 1000, 996, 980};
    private static int[] kingPSQ = {84, 128, 112, 60, 120, 80, 148, 136, 136, 120, 68, 48, 48, 68, 120, 136, 84, 96, 64, 36, 36, 64, 96, 84, 32, 72, 32, 12, 12, 32, 72, 32, 56, 84, 88, 64, 64, 88, 84, 56, 136, 184, 212, 120, 120, 212, 184, 136, 104, 108, 124, 180, 180, 124, 108, 104, 76, 216, 148, 104, 104, 148, 216, 76};
    private static int[] kingEndgamePSQ = {12, 36, 64, 56, 56, 64, 36, 12, 52, 76, 96, 104, 104, 96, 76, 52, 72, 88, 104, 116, 116, 104, 88, 72, 76, 92, 116, 124, 124, 116, 92, 76, 84, 112, 116, 116, 116, 116, 112, 84, 80, 104, 100, 96, 96, 100, 104, 80, 84, 100, 108, 88, 88, 108, 100, 84, 36, 44, 72, 60, 60, 72, 44, 36};
    private static int[] pawnPSQ = {0, 0, 0, 0, 0, 0, 0, 0, 56, 80, 76, 60, 60, 76, 80, 56, 64, 80, 80, 76, 76, 80, 80, 64, 52, 72, 80, 96, 96, 80, 72, 52, 56, 88, 84, 104, 104, 84, 88, 56, 56, 64, 92, 64, 64, 92, 64, 56, -104, -40, -64, -28, -28, -64, -40, -104, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int[] pawnEndgamePSQ = {0, 0, 0, 0, 0, 0, 0, 0, 88, 84, 92, 92, 92, 92, 84, 88, 84, 80, 80, 84, 84, 80, 80, 84, 96, 88, 80, 76, 76, 80, 88, 96, 112, 96, 88, 72, 72, 88, 96, 112, 140, 136, 104, 84, 84, 104, 136, 140, 232, 200, 168, 152, 152, 168, 200, 232, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int[] pawnBonus = {66, 77, 87, 91, 95, 100, 107, 120, 143};
    private static int[] pawnRace = {4, 5, 19, 40, 88, 164};
    private static int ISOLATED_PAWN_PENALTY = 8;
    private static int DOUBLED_PAWN_PENALTY = 14;
    private static int CONNECTED_PAWN_BONUS = 5;
    private static int[] PAWN_SHELTER_BONUS = {-20, -8, 4, 20, 17};

    public static final int PARAMETER_COUNT = 411;

    static {
        applyWeights(new int[PARAMETER_COUNT]);
    }

    public static Map<String, Object> getParameters(){
        Map<String, Object> parameterMap = new LinkedHashMap<>();
        parameterMap.put("knightPSQ", _knightPSQ);
        parameterMap.put("knightEndgamePSQ", _knightEndgamePSQ);
        parameterMap.put("bishopPSQ", _bishopPSQ);
        parameterMap.put("bishopEndgamePSQ", _bishopEndgamePSQ);
        parameterMap.put("rookPSQ", _rookPSQ);
        parameterMap.put("rookEndgamePSQ", _rookEndgamePSQ);
        parameterMap.put("queenPSQ", _queenPSQ);
        parameterMap.put("queenEndgamePSQ", _queenEndgamePSQ);
        parameterMap.put("kingPSQ", _kingPSQ);
        parameterMap.put("kingEndgamePSQ", _kingEndgamePSQ);
        parameterMap.put("pawnPSQ", _pawnPSQ);
        parameterMap.put("pawnEndgamePSQ", _pawnEndgamePSQ);
        parameterMap.put("pawnBonus", _pawnBonus);
        parameterMap.put("pawnRace", _pawnRace);
        parameterMap.put("ISOLATED_PAWN_PENALTY", _ISOLATED_PAWN_PENALTY);
        parameterMap.put("DOUBLED_PAWN_PENALTY", _DOUBLED_PAWN_PENALTY);
        parameterMap.put("CONNECTED_PAWN_BONUS", _CONNECTED_PAWN_BONUS);
        parameterMap.put("PAWN_SHELTER_BONUS", _PAWN_SHELTER_BONUS);

        return parameterMap;
    }

}
