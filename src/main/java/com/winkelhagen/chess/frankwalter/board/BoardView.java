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
package com.winkelhagen.chess.frankwalter.board;

import com.winkelhagen.chess.frankwalter.util.Piece;
import com.winkelhagen.chess.frankwalter.util.Square;
import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;
import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class BoardView {

    private static final Logger logger = LogManager.getLogger();

    private static final String[] STM_STRING = { "WHITE", "BLACK" };

    private Board board;

    public BoardView(Board board) {
        this.board = board;
    }

    public boolean sanityCheck() {
        long[] ctrl = new long[2];
        for (int i = 0; i < 2; i++) {
            ctrl[i] = 0;
            for (Piece piece : Piece.values()) {
                ctrl[i] = addPieceToControlBitboard(ctrl[i], i, piece.getPieceID());
            }
            if (ctrl[i] != board.getPieces()[i][Constants.ALL]) {
                logger.error("ALL bitboards don't match all bitboards for color {}.", i);
                return false;
            }
        }
        if ((board.getPieces()[Constants.WHITE][Constants.ALL] & board.getPieces()[Constants.BLACK][Constants.ALL]) != 0) {
            logger.error("WHITE and BLACK bitboards overlap!");
            return false;
        }
        if ((board.getPieces()[Constants.WHITE][Constants.ALL] | board.getPieces()[Constants.BLACK][Constants.ALL]) != board.getOccupied()) {
            logger.error("WHITE and BLACK bitboards do not cover occupied!");
            return false;
        }
        return verifySquares();
    }

    private long addPieceToControlBitboard(long ctrl, int color, int piece) {
        if ((ctrl & board.getPieces()[color][piece]) != 0) {
            throw new IllegalStateException("Piece bitboards overlap for color " + color + "");
        }
        return ctrl | board.getPieces()[color][piece];
    }

    private boolean verifySquares() {
        for (int i = 0; i < 64; i++) {
            long bit = 1L << i;
            if (board.getSquares()[i] == 0) {
                if ((bit & board.getOccupied()) != 0) {
                    logger.error("square {} is occupied but empty!", Square.byNumber(i));
                    return false;
                }
            } else {
                if ((bit & (board.getPieces()[Constants.BLACK][board.getSquares()[i]] | board.getPieces()[Constants.WHITE][board.getSquares()[i]])) == 0) {
                    logger.error("square {} is has a {} but bitboards say otherwise!", Square.byNumber(i), Piece.getCharFromPieceID(board.getSquares()[i]));
                    return false;
                }
            }
        }
        return true;
    }

    public String getFEN(boolean simple) {
        int[] squares = board.getSquares();
        long[][] pieces = board.getPieces();
        StringBuilder fenBuilder = new StringBuilder();
        int emptySpace = 0;
        for (int row = 7; row >= 0; row--) {
            if (row != 7) {
                if (emptySpace != 0) {
                    fenBuilder.append(emptySpace);
                }
                fenBuilder.append("/");
                emptySpace = 0;
            }
            for (int column = 0; column < 8; column++) {
                emptySpace = appendFENPositionLine(squares, pieces, fenBuilder, emptySpace, row, column);
            }
        }
        if (emptySpace != 0) {
            fenBuilder.append(emptySpace);
        }
        appendFENSideToMove(fenBuilder);
        appendFENCastle(fenBuilder);
        appendFENEp(fenBuilder);
        if (!simple) {
            appendFENExtras(fenBuilder);
        }
        return fenBuilder.toString();
    }

    private int appendFENPositionLine(int[] squares, long[][] pieces, StringBuilder fenBuilder, int emptySpaceInput,
            int row, int column) {
        int emptySpace = emptySpaceInput;
        int square = row * 8 + column;
        if (squares[square] == Constants.EMPTY) {
            emptySpace++;
        } else {
            if (emptySpace != 0) {
                fenBuilder.append(emptySpace);
                emptySpace = 0;
            }
            boolean isWhite = (pieces[Constants.WHITE][Constants.ALL] & BB.single(square)) == 0;
            String pieceChar = Piece.getCharFromPieceID(squares[square]);
            if (isWhite) {
                fenBuilder.append(pieceChar.toLowerCase());
            } else {
                fenBuilder.append(pieceChar.toUpperCase());
            }
        }
        return emptySpace;
    }

    private void appendFENSideToMove(StringBuilder fenBuilder) {
        int sideToMove = board.getSideToMove();
        if (sideToMove == Constants.WHITE) {
            fenBuilder.append(" w");
        } else {
            fenBuilder.append(" b");
        }
    }

    private void appendFENCastle(StringBuilder fenBuilder) {
        int castleMask = board.getCastleMask();
        if (castleMask != 0) {
            fenBuilder.append(" ");
            if ((castleMask & 2) != 0) {
                fenBuilder.append("K");
            }
            if ((castleMask & 1) != 0) {
                fenBuilder.append("Q");
            }
            if ((castleMask & 8) != 0) {
                fenBuilder.append("k");
            }
            if ((castleMask & 4) != 0) {
                fenBuilder.append("q");
            }
        } else {
            fenBuilder.append(" -");
        }
    }

    private void appendFENEp(StringBuilder fenBuilder) {
        int epSquare = board.getEpSquare();
        int sideToMove = board.getSideToMove();
        if (epSquare == -1) {
            fenBuilder.append(" -");
        } else {
            int epSquare2;
            if (sideToMove == Constants.WHITE) {
                epSquare2 = epSquare + 8;
            } else {
                epSquare2 = epSquare - 8;
            }
            fenBuilder.append(" ");
            fenBuilder.append(Square.byNumber(epSquare2));
        }
    }

    private void appendFENExtras(StringBuilder fenBuilder) {
        fenBuilder.append(" ");
        fenBuilder.append(board.getQuiet50());
        fenBuilder.append(" ");
        fenBuilder.append(board.getFullMoves());
    }

    public void echoPosition() {
        StringBuilder sb = new StringBuilder();
        int row = 7;
        sb.append("* Position * * Statistics\n");
        sb.append("*          * *           \n");
        long[][] pieces = board.getPieces();
        int[] squares = board.getSquares();
        echoRow(sb, row--, pieces, squares);
        sb.append(" * * Site to move = " + STM_STRING[board.getSideToMove()] + "\n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" * * WhitePieces = \n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" * * BlackPieces = \n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" * * CastleMask = " + board.getCastleMask() + "\n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" * * EPSquare = " + board.getEpSquare() + "\n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" *\n");
        echoRow(sb, row--, pieces, squares);
        sb.append(" *\n");
        echoRow(sb, row, pieces, squares);
        sb.append(" *\n");
        OutputPrinter.printOutput(sb.toString());
        echoAttackBoards();
    }

    /**
     * 
     */
    private void echoAttackBoards() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                sb.append(Long.bitCount(board.getAttacked()[rank * 8 + file]) % 10);
            }
            sb.append("   ");
            for (int file = 0; file < 8; file++) {
                sb.append(Long.bitCount(board.getAttacking()[rank * 8 + file]) % 10);
            }
            sb.append("\n");
        }
        OutputPrinter.printOutput(sb.toString());
    }

    /**
     * output a row of the board to the screen (in a visual manner)
     * 
     * @param squares
     */
    private void echoRow(StringBuilder sb, int row, long[][] pieces, int[] squares) {
        sb.append("* ");
        long iBit = 1L << (row * 8);
        for (int column = 0; column < 8; column++) {
            if ((iBit & pieces[Constants.WHITE][Constants.ALL]) != 0) {
                sb.append(Piece.getCharFromPieceID(squares[row * 8 + column]).toUpperCase());
            } else if ((iBit & pieces[Constants.BLACK][Constants.ALL]) != 0) {
                sb.append(Piece.getCharFromPieceID(squares[row * 8 + column]).toLowerCase());
            } else if ((row + column) % 2 == 0) {
                sb.append('.');
            } else {
                sb.append(' ');
            }
            iBit <<= 1;
        }
    }

}
