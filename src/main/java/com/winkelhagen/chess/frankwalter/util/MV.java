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

import com.winkelhagen.chess.frankwalter.engine.moves.FailHighLow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;

/**
 * class with helper functions for moves, enabling an int to function as a move.
 *
 * MoveGenerator generated moves should only fill fromSquare, toSquare, promotion and special.
 * 
 * anatomy of a move:
 * 
 * <pre>
 * Field        :   bits
 * fromSquare   =   1-6     (64)
 * toSquare     =   7-12    (64)
 * promotion    =   13-15   (8)
 * captured     =   16-18   (8)
 * special      =   19      (2)
 * score        =   20-29   (1024)
 * unused       =   30-32   (8)
 * </pre>
 * 
 * make sure the most significant bits (excluding the last one?? which should stay unused?) are for the score (which
 * should be non-negative at this point??).
 * 
 * @author Lau
 */
public class MV {
    /**
     * the bit indicating a special move inside a move-integer
     */
    public static final int SPECIAL_BIT = 1 << 18;
    /**
     * the bits in a square inside a move-integer
     */
    public static final int SQUARE_BITS = 63;
    /**
     * the bits representing FROM and TO inside a move-integer
     */
    public static final int FROM_TO_BITS = 4095;
    /**
     * the bits in a piece inside a move-integer
     */
    public static final int PIECE_BITS = 7;
    /**
     * the bits in the move that do not convey the score
     */
    private static final int SCORELESS_MASK = (1 << 19) -1;

    private static Logger logger = LogManager.getLogger();

    /**
     * convenience array with piecenames for each piece.
     */
    private static final String[] pieceName = { "", "q", "r", "b", "n", "", "" };

    /**
     * private constructor
     */
    private MV(){
        
    }
    /**
     * constructs a simple move
     * 
     * @param fromSquare
     *            the departing square
     * @param toSquare
     *            the destination square
     * @return a simple move
     */
    public static int getMove(int fromSquare, int toSquare) {
        return fromSquare | (toSquare << 6);
    }

    /**
     * constructs a simple move with optional promotion
     *
     * @param fromSquare
     *            the departing square
     * @param toSquare
     *            the destination square
     * @param promotes
     *            the promotion piece
     * @return a simple move with optional promotion
     */
    public static int getMove(int fromSquare, int toSquare, int promotes) {
        return fromSquare | (toSquare <<6) | (promotes << 12);
    }


    /**
     * constructs a special move
     * 
     * @param fromSquare
     *            the departing square
     * @param toSquare
     *            the destination square
     * @return a simple move with the special bit set.
     */
    public static int getSpecialMove(int fromSquare, int toSquare) {
        return fromSquare | (toSquare << 6) | SPECIAL_BIT;
    }

    /**
     * determine if the move is special
     * 
     * @param move
     *            the move
     * @return true if the special bit was set, false otherwise.
     */
    public static boolean getSpecial(int move) {
        return (move & SPECIAL_BIT) != 0;
    }

    /**
     * determine the from / to squares of a move
     * 
     * @param move
     *            the move
     * @return the from / to squares of the piece making the move
     */
    public static int getFromTo(int move) {
        return move & FROM_TO_BITS;
    }

    /**
     * determine the departure square of a move
     *
     * @param move
     *            the move
     * @return the departure square of the piece making the move
     */
    public static int getFromSquare(int move) {
        return move & SQUARE_BITS;
    }

    /**
     * determine the destination square of a move
     * 
     * @param move
     *            the move
     * @return the destination square of the piece making the move
     */
    public static int getToSquare(int move) {
        return (move >>> 6) & SQUARE_BITS;
    }

    /**
     * determine the piece the pawn moving will promote into
     * 
     * @param move
     *            the move
     * @return 1,2,3,4 - depending on which piece the moving pawn will promote into
     */
    public static int getPromotion(int move) {
        return (move >>> 12) & PIECE_BITS;
    }

    /**
     * return the piece captured in this move - probably we only know this after the move is done on the board..
     * 
     * @param move
     *            the move
     * @return the captured piece
     */
    public static int getCaptured(int move) {
        return (move >>> 15) & PIECE_BITS;
    }

    /**
     * update the move with information about the capture
     * 
     * @param move
     *            the move
     * @param piece
     *            the captured piece
     * @return the resulting move
     */
    public static int setCaptured(int move, int piece) {
        return move | (piece << 15);
    }

    /**
     * update the move with information about the promotion
     * 
     * @param move
     *            the move
     * @param piece
     *            the piece the pawn will promote into
     * @return the resulting move
     */
    public static int setPromotion(int move, int piece) {
        return move | (piece << 12);
    }

    /**
     * update the move with information about the promotion
     *
     * @param move
     *            the move
     * @param score
     *            the score
     * @return the resulting move
     */
    public static int setScore(int move, int score) {
        return move | (score << 19);
    }

    /**
     * return a string representing the move.
     * 
     * @param move
     *            the move
     * @return a String representation of the move.
     */
    public static String toString(int move) {
        return "" + Square.byNumber(getFromSquare(move)) + Square.byNumber(getToSquare(move)) + pieceName[getPromotion(move)];
    }

    /**
     * return a string representing the pv.
     * 
     * @param principalVariation the principalVariation
     * @param fail a possible indication of a fail-high or fail low
     * @return a String representation of the principalVariation.
     */
    public static String toString(int[] principalVariation, FailHighLow fail) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        boolean endOfPV = false;
        for (int pvMove : principalVariation) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            if (pvMove == 0) {
                endOfPV = true;
            }
            if (endOfPV){
                break;
            }
            sb.append(MV.toString(pvMove));
        }
        if (fail!=null){
            sb.append(fail.getNotation());
        }
        return sb.toString();
    }

    /**
     * print the move to the screen in a full line in full notation, prefixed with "move ".<br>
     *
     * examples: "move b1c3", "move e1g1", move "e7e8q"
     * 
     * @param move
     *            the move
     */
    public static void outputMove(int move) {
        logger.debug("output: move {}", toString(move));
        OutputPrinter.printOutput("move " + toString(move));
    }

    public static int toBasicSquare(String input) {
        if (!input.matches("[a-h][1-8]")) {
            logger.error("unexpected character in square information");
            return -1;
        }
        return ((int) input.charAt(0)) - ((int) 'a') + (((int) input.charAt(1)) - ((int) '1')) * 8;
    }

    /**
     * convert an input String into a basic move, do not check captures or other special stuff except promo's. input in
     * full notation.
     * 
     * @param input
     *            "b1c3", "e1g1", "e7e8q" like strings
     * @return a basic move
     */
    public static int toBasicMove(String input) {
        int move = 0;
        move |= toBasicSquare(input.substring(0,2));
        move |= (toBasicSquare(input.substring(2,4))) << 6;
        if (input.length() == 5) {
            switch (input.toLowerCase().charAt(4)) {
            case 'q':
                move |= Constants.QUEEN << 12;
                break;
            case 'r':
                move |= Constants.ROOK << 12;
                break;
            case 'b':
                move |= Constants.BISHOP << 12;
                break;
            case 'n':
                move |= Constants.KNIGHT << 12;
                break;
            default:
                logger.error("unexpected character in promotion information");
                return 0;
            }

        }
        return move;
    }

    /**
     * determines if a user move is equal to another move
     * 
     * @param userMove
     *            the user-inputted move
     * @param move
     *            the move from the movelist.
     * @return true if the usermove positively identifies the other move.
     */
    public static boolean match(int userMove, int move) {
        return (getToSquare(move) == getToSquare(userMove) && getFromSquare(move) == getFromSquare(userMove))
                && getPromotion(move) == getPromotion(userMove);
    }

    /**
     * convert a rownumber char into the actual row number
     * 
     * @param c
     *            the char '1' thru '8'
     * @return '0' thru '7' (or -1 for no row)
     */
    public static int rankFromChar(char c) {
        int rank = ((int) c) - ((int) '1');
        if (rank>7 || rank < 0){
            return -1;
        } else {
            return rank;
        }
    }

    /**
     * convert a file char into the actual file number
     * 
     * @param c
     *            the char 'a' thru 'h'
     * @return '0' thru '7' (or -1 for no file)
     */
    public static int fileFromChar(char c) {
        int file = ((int)c) - ((int) 'a');
        if (file>7 || file < 0){
            return -1;
        } else {
            return file;
        }
    }

    /**
     * return the move without the move ordering score
     * @param move the scored move.
     * @return the move stripped of the score
     */
    public static int stripScore(int move) {
        return move & SCORELESS_MASK;
    }

    /**
     * returns the score associated with this ordered move
     * @param move the move
     * @return the score component
     */
    public static int getScore(int move) {
        return (move >> 19) & 1023;
    }
}
