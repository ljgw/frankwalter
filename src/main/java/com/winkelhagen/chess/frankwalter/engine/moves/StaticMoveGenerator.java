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
package com.winkelhagen.chess.frankwalter.engine.moves;

import com.winkelhagen.chess.frankwalter.util.Piece;
import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;

public class StaticMoveGenerator {

    private StaticMoveGenerator() {
        //empty
    }

    public static int generateMoves(Board board, int[] moves) {
        int sideToMove = board.getSideToMove();
        int numberOfMoves = 0;

        // Find castling moves.
        numberOfMoves = generateCastlingMoves(sideToMove, board, numberOfMoves, moves);
        long pins = board.getPins(sideToMove);
        long pawns = board.getPieces()[sideToMove][Constants.PAWN] & ~pins;
        long movers = board.getPieces()[sideToMove][Constants.ALL] & ~pawns
                & ~board.getPieces()[sideToMove][Constants.KING] & ~pins;
        long targets = ~board.getPieces()[sideToMove][Constants.ALL];
        while (movers != 0) {
            int fromSquare = BB.lsb(movers);
            movers &= movers - 1;
            long newMoves = board.getAttacking()[fromSquare] & targets;
            while (newMoves != 0) {
                int toSquare = BB.lsb(newMoves);
                newMoves &= newMoves - 1;
                moves[numberOfMoves++] = MV.getMove(fromSquare, toSquare);
            }
        }
        // Kings get special treatment
        int kingSquare = board.getKings(sideToMove);
        long kingMoves = board.getAttacking()[kingSquare] & targets;
        long enemyPieces = board.getPieces()[sideToMove ^ 1][Constants.ALL];
        while (kingMoves != 0) {
            int toSquare = BB.lsb(kingMoves);
            kingMoves &= kingMoves - 1;
            if ((board.getAttacked()[toSquare] & enemyPieces) == 0) {
                moves[numberOfMoves++] = MV.getMove(kingSquare, toSquare);
            }
        }

        if (pins != 0) {
            // pinned knights cannot move
            pins &= ~board.getPieces()[sideToMove][Constants.KNIGHT];
            long kingAttackSquaresBishop = BB.getBishopMoves(kingSquare, enemyPieces);
            long kingAttackSquaresRook = BB.getRookMoves(kingSquare, enemyPieces);
            while (pins != 0) {
                int pinnedPiece = BB.lsb(pins);
                long pinnedPieceBB = BB.single(pinnedPiece);
                int pinnedPieceType = board.getSquares()[pinnedPiece];
                long newMoves = 0;
                switch (pinnedPieceType) {
                case (Constants.PAWN):
                    if (BB.fileOf(kingSquare) == BB.fileOf(pinnedPiece)) {
                        if (sideToMove == Constants.WHITE) {
                            if ((pinnedPieceBB << 8 & board.getOccupied()) == 0) {
                                moves[numberOfMoves++] = MV.getMove(pinnedPiece, pinnedPiece + 8);
                                if (BB.rankOf(pinnedPiece) == 1) {
                                    if ((pinnedPieceBB << 16 & board.getOccupied()) == 0) {
                                        moves[numberOfMoves++] = MV.getSpecialMove(pinnedPiece, pinnedPiece + 16);
                                    }
                                }
                            }
                        } else {
                            if ((pinnedPieceBB >> 8 & board.getOccupied()) == 0) {
                                moves[numberOfMoves++] = MV.getMove(pinnedPiece, pinnedPiece - 8);
                                if (BB.rankOf(pinnedPiece) == 6) {
                                    if ((pinnedPieceBB >> 16 & board.getOccupied()) == 0) {
                                        moves[numberOfMoves++] = MV.getSpecialMove(pinnedPiece, pinnedPiece - 16);
                                    }
                                }
                            }
                        }
                    } else if ((kingAttackSquaresBishop & pinnedPieceBB) != 0) {
                        long epBB = 0;
                        if (board.getEpSquare() != -1) {
                            epBB = BB.single(board.getEpSquare());
                        }
                        long validPawnAttack = kingAttackSquaresBishop & board.getAttacking()[pinnedPiece]
                                & (enemyPieces | epBB);
                        if (validPawnAttack != 0) {
                            int toSquare = BB.lsb(validPawnAttack);
                            if (validPawnAttack == epBB) {
                                moves[numberOfMoves++] = MV.getSpecialMove(pinnedPiece, toSquare);
                            } else {

                                if (BB.rankOf(toSquare) == 0 || BB.rankOf(toSquare) == 7) {
                                    int move = MV.getSpecialMove(pinnedPiece, toSquare);
                                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                                } else {
                                    moves[numberOfMoves++] = MV.getMove(pinnedPiece, toSquare);
                                }
                            }
                        }
                    }
                    break;
                case (Constants.QUEEN):
                    if ((kingAttackSquaresRook & pinnedPieceBB) != 0) {
                        newMoves = BB.getRookMoves(pinnedPiece, ~kingAttackSquaresRook) & kingAttackSquaresRook;
                    } else {
                        newMoves = BB.getBishopMoves(pinnedPiece, ~kingAttackSquaresBishop) & kingAttackSquaresBishop;
                    }
                    break;
                case (Constants.ROOK):
                    if ((kingAttackSquaresRook & pinnedPieceBB) != 0) {
                        newMoves = kingAttackSquaresRook & board.getAttacking()[pinnedPiece];
                    }
                    break;
                case (Constants.BISHOP):
                    if ((kingAttackSquaresBishop & pinnedPieceBB) != 0) {
                        newMoves = kingAttackSquaresBishop & board.getAttacking()[pinnedPiece];
                    }
                    break;
                default:
                    throw new RuntimeException("a king cannot be pinned");
                }
                while (newMoves != 0) {
                    int toSquare = BB.lsb(newMoves);
                    newMoves &= newMoves - 1;
                    moves[numberOfMoves++] = MV.getMove(pinnedPiece, toSquare);
                }
                pins &= pins - 1;
            }
        }
        numberOfMoves = generatePawnMoves(board, moves, numberOfMoves, pawns);
        return numberOfMoves;

    }

    private static int generateCastlingMoves(int sideToMove, Board board, int numberOfMoves, int[] moves) {
        int castleMask = board.getCastleMask();
        if (sideToMove == Constants.WHITE) {
            if ((castleMask & 1) != 0) {
                if ((board.getOccupied() & 7L << 1) == 0 && canCastle(board, 2, sideToMove)) {
                    moves[numberOfMoves++] = MV.getSpecialMove(4, 2);
                }
            }
            if ((castleMask & 2) != 0) {
                if ((board.getOccupied() & 3L << 5) == 0 && canCastle(board, 4, sideToMove)) {
                    moves[numberOfMoves++] = MV.getSpecialMove(4, 6);
                }
            }
        } else {
            if ((castleMask & 4) != 0) {
                if ((board.getOccupied() & 7L << 57) == 0 && canCastle(board, 58, sideToMove)) {
                    moves[numberOfMoves++] = MV.getSpecialMove(60, 58);
                }
            }
            if ((castleMask & 8) != 0) {
                if ((board.getOccupied() & 3L << 61) == 0 && canCastle(board, 60, sideToMove)) {
                    moves[numberOfMoves++] = MV.getSpecialMove(60, 62);
                }
            }
        }
        return numberOfMoves;
    }

    public static int generateUnquiet(Board board, int[] moves) {
        int sideToMove = board.getSideToMove();
        int numberOfMoves = 0;

        long pins = board.getPins(sideToMove);
        long pawns = board.getPieces()[sideToMove][Constants.PAWN] & ~pins;
        long movers = board.getPieces()[sideToMove][Constants.ALL] & ~pawns
                & ~board.getPieces()[sideToMove][Constants.KING] & ~pins;
        long targets = board.getPieces()[sideToMove ^ 1][Constants.ALL];
        while (movers != 0) {
            int fromSquare = BB.lsb(movers);
            movers &= movers - 1;
            long newMoves = board.getAttacking()[fromSquare] & targets;
            while (newMoves != 0) {
                int toSquare = BB.lsb(newMoves);
                newMoves &= newMoves - 1;
                moves[numberOfMoves++] = MV.getMove(fromSquare, toSquare);
            }
        }
        // Kings get special treatment
        int kingSquare = board.getKings(sideToMove);
        long kingMoves = board.getAttacking()[kingSquare] & targets;
        long enemyPieces = board.getPieces()[sideToMove ^ 1][Constants.ALL];
        while (kingMoves != 0) {
            int toSquare = BB.lsb(kingMoves);
            kingMoves &= kingMoves - 1;
            if ((board.getAttacked()[toSquare] & enemyPieces) == 0) {
                moves[numberOfMoves++] = MV.getMove(kingSquare, toSquare);
            }
        }

        if (pins != 0) {
            // pinned knights cannot move
            pins &= ~board.getPieces()[sideToMove][Constants.KNIGHT];
            long kingAttackSquaresBishop = BB.getBishopMoves(kingSquare, enemyPieces);
            long kingAttackSquaresRook = BB.getRookMoves(kingSquare, enemyPieces);
            while (pins != 0) {
                int pinnedPiece = BB.lsb(pins);
                long pinnedPieceBB = BB.single(pinnedPiece);
                int pinnedPieceType = board.getSquares()[pinnedPiece];
                long newMoves = 0;
                switch (pinnedPieceType) {
                case (Constants.PAWN):
                    // no normal vertical moves
                    if ((kingAttackSquaresBishop & pinnedPieceBB) != 0) {
                        long epBB = 0;
                        if (board.getEpSquare() != -1) {
                            epBB = BB.single(board.getEpSquare());
                        }
                        long validPawnAttack = kingAttackSquaresBishop & board.getAttacking()[pinnedPiece]
                                & (enemyPieces | epBB);
                        if (validPawnAttack != 0) {
                            int toSquare = BB.lsb(validPawnAttack);
                            if (validPawnAttack == epBB) {
                                moves[numberOfMoves++] = MV.getSpecialMove(pinnedPiece, toSquare);
                            } else {

                                if (BB.rankOf(toSquare) == 0 || BB.rankOf(toSquare) == 7) {
                                    int move = MV.getSpecialMove(pinnedPiece, toSquare);
                                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                                } else {
                                    moves[numberOfMoves++] = MV.getMove(pinnedPiece, toSquare);
                                }
                            }
                        }
                    }
                    break;
                case (Constants.QUEEN):
                    if ((kingAttackSquaresRook & pinnedPieceBB) != 0) {
                        newMoves = BB.getRookMoves(pinnedPiece, ~kingAttackSquaresRook) & kingAttackSquaresRook
                                & targets;
                    } else {
                        newMoves = BB.getBishopMoves(pinnedPiece, ~kingAttackSquaresBishop) & kingAttackSquaresBishop
                                & targets;
                    }
                    break;
                case (Constants.ROOK):
                    if ((kingAttackSquaresRook & pinnedPieceBB) != 0) {
                        newMoves = kingAttackSquaresRook & board.getAttacking()[pinnedPiece] & targets;
                    }
                    break;
                case (Constants.BISHOP):
                    if ((kingAttackSquaresBishop & pinnedPieceBB) != 0) {
                        newMoves = kingAttackSquaresBishop & board.getAttacking()[pinnedPiece] & targets;
                    }
                    break;
                default:
                    throw new RuntimeException("a king cannot be pinned");
                }
                while (newMoves != 0) {
                    int toSquare = BB.lsb(newMoves);
                    newMoves &= newMoves - 1;
                    moves[numberOfMoves++] = MV.getMove(pinnedPiece, toSquare);
                }
                pins &= pins - 1;
            }
        }
        numberOfMoves = generatePawnUnquiet(board, moves, numberOfMoves, pawns);

        // Score and sort moves.
        return numberOfMoves;
    }

    public static int generateOutOfCheckMoves(Board board, int attackingSquare, int[] moves) {
        int numberOfMoves = 0;
        int stm = board.getSideToMove();
        int kingSquare = BB.lsb(board.getPieces()[stm][Constants.KING]);
        long kingless = board.getOccupied() & ~board.getPieces()[stm][Constants.KING];

        // More than one king attacker means we cannot block / capture it: need to move king.
        if (attackingSquare == Constants.MULTIPLE_SQUARES) {
            long kingPossibilities = board.getAttacking()[kingSquare] & ~board.getPieces()[stm][Constants.ALL];
            while (kingPossibilities != 0) {
                int toSquare = BB.lsb(kingPossibilities);
                kingPossibilities &= kingPossibilities - 1;
                if ((board.getAttacked()[toSquare] & board.getPieces()[stm ^ 1][Constants.ALL]) == 0) {
                    if (((BB.getBishopMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                            | board.getPieces()[stm ^ 1][Constants.BISHOP]))
                            | (BB.getRookMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                                    | board.getPieces()[stm ^ 1][Constants.ROOK]))) == 0) {
                        moves[numberOfMoves++] = MV.getMove(kingSquare, toSquare);
                    }
                }
            }
        } else {
            int[] interveningSquares = BB.getInterveningSquares(attackingSquare, kingSquare);
            int numSquares = interveningSquares.length;
            if (numSquares == 1) {
                numberOfMoves = generateCapturesTo(board, moves, numberOfMoves, interveningSquares[0], stm);
                long kingPossibilities = board.getAttacking()[kingSquare] & ~board.getPieces()[stm][Constants.ALL];
                while (kingPossibilities != 0) {
                    int toSquare = BB.lsb(kingPossibilities);
                    kingPossibilities &= kingPossibilities - 1;
                    if ((board.getAttacked()[toSquare] & board.getPieces()[stm ^ 1][Constants.ALL]) == 0) {
                        if (((BB.getBishopMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                                | board.getPieces()[stm ^ 1][Constants.BISHOP]))
                                | (BB.getRookMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                                        | board.getPieces()[stm ^ 1][Constants.ROOK]))) == 0) {
                            moves[numberOfMoves++] = MV.getMove(kingSquare, toSquare);
                        }
                    }
                }
                if (board.getEpSquare() != -1) {
                    int toSquare = board.getEpSquare();
                    if (stm == Constants.WHITE) {
                        toSquare += 8;
                    } else {
                        toSquare -= 8;
                    }
                    long epPossibilities = board.getAttacked()[toSquare]
                            & (board.getPieces()[stm][Constants.PAWN] & ~board.getPins(stm));
                    while (epPossibilities != 0) {
                        int fromSquare = BB.lsb(epPossibilities);
                        epPossibilities &= epPossibilities - 1;
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, toSquare);
                    }
                }
            } else {
                numberOfMoves = generateCapturesTo(board, moves, numberOfMoves, interveningSquares[0], stm);
                long kingPossibilities = board.getAttacking()[kingSquare] & ~board.getPieces()[stm][Constants.ALL];
                while (kingPossibilities != 0) {
                    int toSquare = BB.lsb(kingPossibilities);
                    kingPossibilities &= kingPossibilities - 1;
                    if ((board.getAttacked()[toSquare] & board.getPieces()[stm ^ 1][Constants.ALL]) == 0) {
                        if (((BB.getBishopMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                                | board.getPieces()[stm ^ 1][Constants.BISHOP]))
                                | (BB.getRookMoves(toSquare, kingless) & (board.getPieces()[stm ^ 1][Constants.QUEEN]
                                        | board.getPieces()[stm ^ 1][Constants.ROOK]))) == 0) {
                            moves[numberOfMoves++] = MV.getMove(kingSquare, toSquare);
                        }
                    }
                }
                for (int i = 1; i < numSquares; i++) {
                    numberOfMoves = generateMovesTo(board, moves, numberOfMoves, interveningSquares[i], stm);
                }

            }
        }
        return numberOfMoves;
    }

    private static boolean canCastle(Board board, int startSquare, int stm) {
        if ((board.getAttacked()[startSquare] & board.getPieces()[stm ^ 1][Constants.ALL]) != 0)
            return false;
        if ((board.getAttacked()[startSquare + 1] & board.getPieces()[stm ^ 1][Constants.ALL]) != 0)
            return false;
        if ((board.getAttacked()[startSquare + 2] & board.getPieces()[stm ^ 1][Constants.ALL]) != 0)
            return false;
        return true;
    }

    /**
     * generates pawn moves and captures
     * 
     * @param moves
     *            the move list.
     * @param numberOfMoves
     *            the number of moves in the move list.
     * @param pawns
     *            the pawns of the side to move.
     * @return the new number of moves in the move list.
     */
    private static int generatePawnMoves(Board board, int[] moves, int numberOfMoves, long pawns) {
        int sideToMove = board.getSideToMove();
        while (pawns != 0) {
            long pawn = pawns & (pawns ^ (pawns - 1));
            int fromSquare = BB.lsb(pawn);
            int rank = fromSquare >> 3;
            int file = fromSquare & 7;
            int epSquare = board.getEpSquare();
            pawns &= pawns - 1;
            if (sideToMove == Constants.WHITE) {
                if ((pawn << 8 & board.getOccupied()) == 0) {
                    if (rank == 6) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare + 8);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare + 8);
                    }
                    if (rank == 1) {
                        if ((pawn << 16 & board.getOccupied()) == 0) {
                            moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare + 16);
                        }
                    }
                }
                if (file != 0 && (pawn << 7 & board.getPieces()[Constants.BLACK][Constants.ALL]) != 0) {
                    if (rank == 6) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare + 7);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare + 7);
                    }
                }
                if (file != 0 && fromSquare - 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 4
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.BLACK][Constants.ROOK]
                                            | board.getPieces()[Constants.BLACK][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare + 7);
                    }
                }
                if (file != 7 && (pawn << 9 & board.getPieces()[Constants.BLACK][Constants.ALL]) != 0) {
                    if (rank == 6) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare + 9);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare + 9);
                    }
                }
                if (file != 7 && fromSquare + 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 4
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.BLACK][Constants.ROOK]
                                            | board.getPieces()[Constants.BLACK][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare + 9);
                    }
                }
            } else {
                if ((pawn >> 8 & board.getOccupied()) == 0) {
                    if (rank == 1) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare - 8);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare - 8);
                    }
                    if (rank == 6) {
                        if ((pawn >> 16 & board.getOccupied()) == 0) {
                            moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare - 16);
                        }
                    }
                }
                if (file != 0 && (pawn >> 9 & board.getPieces()[Constants.WHITE][Constants.ALL]) != 0) {
                    if (rank == 1) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare - 9);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare - 9);
                    }
                }
                if (file != 0 && fromSquare - 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 3
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.WHITE][Constants.ROOK]
                                            | board.getPieces()[Constants.WHITE][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare - 9);
                    }
                }
                if (file != 7 && (pawn >> 7 & board.getPieces()[Constants.WHITE][Constants.ALL]) != 0) {
                    if (rank == 1) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare - 7);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare - 7);
                    }
                }
                if (file != 7 && fromSquare + 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 3
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.WHITE][Constants.ROOK]
                                            | board.getPieces()[Constants.WHITE][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare - 7);
                    }
                }
            }
        }
        return numberOfMoves;
    }

    /**
     * generates pawn captures
     * 
     * @param moves
     *            the move list.
     * @param numberOfMoves
     *            the number of moves in the move list.
     * @pawns the pawns
     * @return the new number of moves in the move list.
     */
    private static int generatePawnUnquiet(Board board, int[] moves, int numberOfMoves, long pawns) {
        int sideToMove = board.getSideToMove();
        while (pawns != 0) {
            long pawn = pawns & (pawns ^ (pawns - 1));
            int fromSquare = BB.lsb(pawn);
            int rank = fromSquare >> 3;
            int file = fromSquare & 7;
            int epSquare = board.getEpSquare();
            pawns &= pawns - 1;
            if (sideToMove == Constants.WHITE) {
                if (file != 0 && (pawn << 7 & board.getPieces()[Constants.BLACK][Constants.ALL]) != 0) {
                    if (rank == 6) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare + 7);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare + 7);
                    }
                }
                if (file != 0 && fromSquare - 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 4
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.BLACK][Constants.ROOK]
                                            | board.getPieces()[Constants.BLACK][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare + 7);
                    }
                }
                if (file != 7 && (pawn << 9 & board.getPieces()[Constants.BLACK][Constants.ALL]) != 0) {
                    if (rank == 6) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare + 9);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare + 9);
                    }
                }
                if (file != 7 && fromSquare + 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 4
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.BLACK][Constants.ROOK]
                                            | board.getPieces()[Constants.BLACK][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare + 9);
                    }
                }
            } else {
                if (file != 0 && (pawn >> 9 & board.getPieces()[Constants.WHITE][Constants.ALL]) != 0) {
                    if (rank == 1) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare - 9);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare - 9);
                    }
                }
                if (file != 0 && fromSquare - 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 3
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.WHITE][Constants.ROOK]
                                            | board.getPieces()[Constants.WHITE][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare - 9);
                    }
                }
                if (file != 7 && (pawn >> 7 & board.getPieces()[Constants.WHITE][Constants.ALL]) != 0) {
                    if (rank == 1) {
                        int move = MV.getSpecialMove(fromSquare, fromSquare - 7);
                        moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    } else {
                        moves[numberOfMoves++] = MV.getMove(fromSquare, fromSquare - 7);
                    }
                }
                if (file != 7 && fromSquare + 1 == epSquare) {
                    int kingSquare = board.getKings(sideToMove);
                    if (BB.rankOf(kingSquare) != 3
                            || (BB.getRookMoves(kingSquare, board.getOccupied() & ~(BB.single(epSquare) | pawn))
                                    & (board.getPieces()[Constants.WHITE][Constants.ROOK]
                                            | board.getPieces()[Constants.WHITE][Constants.QUEEN])) == 0) {
                        moves[numberOfMoves++] = MV.getSpecialMove(fromSquare, fromSquare - 7);
                    }
                }
            }
        }
        return numberOfMoves;
    }

    /**
     * find all captures (non king) that venture into the <i>square</i> to capture the attacker trying to take our
     * king.</br>
     * doesn't generate ep captures! (due to the fact that these can never be valid moves to get out of check).</br>
     * 
     * @param moves
     *            the movelist to add to.
     * @param numberOfMoves
     *            the number of moves currently in the movelist
     * @param square
     *            the square we're trying to capture - to kill the piece that attacks our king.
     * @param stm
     *            the side to move.
     * @return the new number of moves in the movelist
     */
    private static int generateCapturesTo(Board board, int[] moves, int numberOfMoves, int square, int stm) {
        long squareBB = BB.single(square);
        long pinned = board.getPins(stm);
        int file = square & 7;
        // Generate captures by pawns.
        if (stm == Constants.WHITE) {
            if (file != 7 && (squareBB & (board.getPieces()[Constants.WHITE][Constants.PAWN] & ~pinned) << 7) != 0) {
                if (BB.rankOf(square) == 7) {
                    int move = MV.getSpecialMove(square - 7, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square - 7, square);
                }
            }
            if (file != 0 && (squareBB & (board.getPieces()[Constants.WHITE][Constants.PAWN] & ~pinned) << 9) != 0) {
                if (BB.rankOf(square) == 7) {
                    int move = MV.getSpecialMove(square - 9, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square - 9, square);
                }
            }
        } else {
            if (file != 0 && (squareBB & (board.getPieces()[Constants.BLACK][Constants.PAWN] & ~pinned) >> 7) != 0) {
                if (BB.rankOf(square) == 0) {
                    int move = MV.getSpecialMove(square + 7, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square + 7, square);
                }
            }
            if (file != 7 && (squareBB & (board.getPieces()[Constants.BLACK][Constants.PAWN] & ~pinned) >> 9) != 0) {
                if (BB.rankOf(square) == 0) {
                    int move = MV.getSpecialMove(square + 9, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square + 9, square);
                }
            }
        }
        // Generate captures by other pieces excluding kings
        long attackers = BB.knight[square] & board.getPieces()[stm][Constants.KNIGHT];
        attackers |= BB.getBishopMoves(square, board.getOccupied())
                & (board.getPieces()[stm][Constants.BISHOP] | board.getPieces()[stm][Constants.QUEEN]);
        attackers |= BB.getRookMoves(square, board.getOccupied())
                & (board.getPieces()[stm][Constants.ROOK] | board.getPieces()[stm][Constants.QUEEN]);
        attackers &= ~pinned;
        while (attackers != 0) {
            int attacker = BB.lsb(attackers);
            moves[numberOfMoves++] = MV.getMove(attacker, square);
            attackers &= attackers - 1;
        }

        return numberOfMoves;
    }

    /**
     * find all moves (non-capture, non king) that venture into the <i>square</i> to block an attacker from taking our
     * king.</br>
     * 
     * @param moves
     *            the movelist to add to.
     * @param numberOfMoves
     *            the number of moves currently in the movelist
     * @param square
     *            the square we're trying to capture - to kill the piece that attacks our king.
     * @param stm
     *            the side to move.
     * @return the new number of moves in the movelist
     */
    private static int generateMovesTo(Board board, int[] moves, int numberOfMoves, int square, int stm) {
        long squareBB = BB.single(square);
        long pinned = board.getPins(stm);
        // Generate moves by pawns
        if (stm == Constants.WHITE) {
            if ((squareBB & (board.getPieces()[Constants.WHITE][Constants.PAWN] & ~pinned) << 8) != 0) {
                if (BB.rankOf(square) == 7) {
                    int move = MV.getSpecialMove(square - 8, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square - 8, square);
                }
            } else if (BB.rankOf(square) == 3 && ((squareBB & (board.getOccupied() << 8)) == 0)) {
                if ((squareBB & (board.getPieces()[Constants.WHITE][Constants.PAWN] & ~pinned) << 16) != 0) {
                    moves[numberOfMoves++] = MV.getSpecialMove(square - 16, square);
                }
            }
        } else {
            if ((squareBB & (board.getPieces()[Constants.BLACK][Constants.PAWN] & ~pinned) >> 8) != 0) {
                if (BB.rankOf(square) == 0) {
                    int move = MV.getSpecialMove(square + 8, square);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.QUEEN);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.ROOK);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.BISHOP);
                    moves[numberOfMoves++] = MV.setPromotion(move, Constants.KNIGHT);
                } else {
                    moves[numberOfMoves++] = MV.getMove(square + 8, square);
                }
            } else if (BB.rankOf(square) == 4 && ((squareBB & (board.getOccupied() >> 8)) == 0)) {
                if ((squareBB & (board.getPieces()[Constants.BLACK][Constants.PAWN] & ~pinned) >> 16) != 0) {
                    moves[numberOfMoves++] = MV.getSpecialMove(square + 16, square);
                }
            }
        }

        // Generate moves by other pieces excluding kings
        long attackers = BB.knight[square] & board.getPieces()[stm][Constants.KNIGHT];
        attackers |= BB.getBishopMoves(square, board.getOccupied())
                & (board.getPieces()[stm][Constants.BISHOP] | board.getPieces()[stm][Constants.QUEEN]);
        attackers |= BB.getRookMoves(square, board.getOccupied())
                & (board.getPieces()[stm][Constants.ROOK] | board.getPieces()[stm][Constants.QUEEN]);
        attackers &= ~pinned;
        while (attackers != 0) {
            int attacker = BB.lsb(attackers);
            moves[numberOfMoves++] = MV.getMove(attacker, square);
            attackers &= attackers - 1;
        }

        return numberOfMoves;
    }

    public static int getKingAttacker(Board board, int stm) {
        int kingSquare = board.getKings(stm ^ 1);
        long kingAttackers = (board.getAttacked()[kingSquare] & board.getPieces()[stm][Constants.ALL]);

        if (kingAttackers == 0)
            return Constants.NO_SQUARE;
        int kingAttacker = BB.lsb(kingAttackers);
        kingAttackers &= kingAttackers - 1;
        if (kingAttackers == 0)
            return kingAttacker;
        return Constants.MULTIPLE_SQUARES;

        // int numberOfAttackers = Long.bitCount(kingAttackers);
        // if (numberOfAttackers==0) return return Constants.NO_SQUARE;
        // if (numberOfAttackers==1) return BB.lsb(kingAttackers);
        // return Constants.MULTIPLE_SQUARES;
    }

    public static boolean isKingAttacked(Board board, int stm) {
        int kingSquare = BB.lsb(board.getPieces()[stm ^ 1][Constants.KING]);
        return (board.getAttacked()[kingSquare] & board.getPieces()[stm][Constants.ALL]) != 0;

    }

    public static int parseSAN(Board board, String san) {
        int[] moves = new int[100];
        int movesNr = generateMoves(board, moves);
        int move = 0;
        if (san.charAt(0) == 'O') {
            if (board.getSideToMove() == Constants.WHITE) {
                if (san.startsWith("O-O"))
                    move = MV.toBasicMove("e1g1");
                if (san.startsWith("O-O-O"))
                    move = MV.toBasicMove("e1c1");
            }
            if (board.getSideToMove() == Constants.BLACK) {
                if (san.startsWith("O-O"))
                    move = MV.toBasicMove("e8g8");
                if (san.startsWith("O-O-O"))
                    move = MV.toBasicMove("e8c8");
            }
            for (int i = 0; i < movesNr; i++) {
                if (moves[i] == 0)
                    break;
                if (MV.match(move, moves[i])) {
                    return moves[i];
                }
            }
        }
        int toRank = -1;
        int crux = 0;
        int promotion = 0;
        int length = san.length();
        // find the destination square
        for (int i = length - 1; i >= 0; i--) {
            toRank = MV.rankFromChar(san.charAt(i));
            if (toRank != -1) {
                crux = i;
                break;
            }
        }
        int toFile = MV.fileFromChar(san.charAt(crux - 1));
        int toSquare = toRank * 8 + toFile;
        if (crux + 1 < length && san.charAt(crux + 1) == '=') {
            promotion = Piece.getPieceIDFromSAN(san.charAt(crux + 2));
        }
        int piece = Piece.getPieceIDFromSAN(san.charAt(0));
        int fromFile = -1;
        int fromRank = -1;

        // All pawn moves
        if (piece == Constants.PAWN) {
            if (crux - 1 == 0) {
                fromFile = toFile;
            } else {
                fromFile = MV.fileFromChar(san.charAt(crux - 3));
            }
            for (int i = 0; i < movesNr; i++) {
                if (moves[i] == 0)
                    break;
                if (MV.getToSquare(moves[i]) == toSquare) {
                    int possibleFromSquare = MV.getFromSquare(moves[i]);
                    if (fromFile == BB.fileOf(possibleFromSquare)
                            && board.getSquares()[possibleFromSquare] == Constants.PAWN
                            && MV.getPromotion(moves[i]) == promotion) {
                        return moves[i];
                    }
                }
            }
            return 0;
        }
        if (crux - 2 > 0) {
            fromFile = MV.fileFromChar(san.charAt(1));
            if (san.charAt(crux - 2) == 'x') {
                fromRank = MV.rankFromChar(san.charAt(crux - 3));
            } else {
                fromRank = MV.rankFromChar(san.charAt(crux - 2));
            }
        }
        long pieceBB = board.getPieces()[board.getSideToMove()][piece];
        boolean foundMatch = false;
        int matchingMove = 0;
        while (pieceBB != 0) {
            int fromSquare = BB.lsb(pieceBB);
            pieceBB &= pieceBB - 1;
            if (fromFile != -1 && BB.fileOf(fromSquare) != fromFile)
                continue;
            if (fromRank != -1 && BB.rankOf(fromSquare) != fromRank)
                continue;
            move = MV.getMove(fromSquare, toSquare);
            for (int i = 0; i < movesNr; i++) {
                if (moves[i] == 0)
                    break;
                if (MV.match(move, moves[i])) {
                    board.doMove(moves[i]);
                    if (!isKingAttacked(board, board.getSideToMove())){
                        if (foundMatch){
                            board.undoMove();
                            return 0;
                        }
                        foundMatch = true;
                        matchingMove = moves[i];
                    }
                    board.undoMove();
                }
            }
        }
        return matchingMove;
    }

    public static int generateLegalMoves(Board board, int[] generatedMoves) {
        int generatedMovesNr;
        int kingAttacker = getKingAttacker(board, board.getSideToMove() ^ 1);
        if (kingAttacker == Constants.NO_SQUARE) {
            generatedMovesNr = generateMoves(board, generatedMoves);
        } else {
            generatedMovesNr = generateOutOfCheckMoves(board, kingAttacker, generatedMoves);
        }

        return generatedMovesNr;
    }

    public static int findLegalMove(Board board, int bookMove) {
        int[] moveArray = new int[Board.ABSOLUTE_MAX_MOVES];
        int move = 0;
        int movesNr = generateLegalMoves(board, moveArray);
        for (int i = 0; i < movesNr; i++) {
            if (moveArray[i] == 0 || MV.match(bookMove, moveArray[i])) {
                move = moveArray[i];
                break;
            }
        }
        return move;
    }

    public static boolean hasLegalMoves(Board board) {
        int[] moveArray = new int[Board.ABSOLUTE_MAX_MOVES];
        int movesNr = generateLegalMoves(board, moveArray);
        return movesNr != 0;
    }
}
