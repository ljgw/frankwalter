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

import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.Piece;
import com.winkelhagen.chess.frankwalter.engine.tb.Syzygy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;
import com.winkelhagen.chess.frankwalter.util.ZobristHash;

/**
 * class representing the board
 * The board representation is a bitboard variant with attacktables
 *
 */
public class Board {


    /**
     * objects to hold moves to be un-done (including hard to reconstruct states of the board)
     *
     */
    private class UndoableMove {

        int move;
        int quiet50;
        int epSquare;
        int castleMask;
        long hashKey;
        long pinsWhite;
        long pinsBlack;
        long[] attacked = new long[64];
        long[] attacking = new long[64];

    }

    private static final int HISTORYSIZE = 10000;
    private static final int[] pieceValue = { 0, 900, 500, 300, 300, 990, 100 };

    private int[] materialScore = { 0, 0 };
    private int pieceCount;

    /**
     * array of bitboards holding the squares attacked by the indexed square
     */
    private long[] attacking = new long[64];
    /**
     * array of bitboards holding the squares attacking the indexed square
     */
    private long[] attacked = new long[64];

    private static final Logger logger = LogManager.getLogger();

    public static final int ABSOLUTE_MAX_MOVES = 250;

    /* bitboards */
    private long occupied = 0;
    private long[][] pieces = { { 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0 } };
    private int sideToMove = Constants.WHITE;
    private int[] squares = new int[64];
    private int epSquare = -1;
    private int castleMask = 15;

    private int ply = 0;
    private int fullMoves = 0;

    private int[] kings = { 0, 0 };
    private long pinsWhite = 0;
    private long pinsBlack = 0;

    private int quiet50 = 0;

    private ZobristHash zobrist = new ZobristHash();
    private ZobristHash ctrlZobrist = new ZobristHash();

    /**
     * We use a separate array for repetition detection. NullMove hashKeys are not stored, because they at best interfere with repetition detection.
     * For example: three null moves in the same position would result in a repetition being detected and the move would be scored 0 in search
     */
    private long[] hashtory = new long[HISTORYSIZE];

    private UndoableMove[] history = new UndoableMove[HISTORYSIZE];

    /**
     * Constructor: Returns a new board instance and resets it.
     */
    public Board() {
        //idea is to use assertions as a debug mechanism. It will slow the engine down a lot because hashkeys are recalculated every move
        if (getClass().desiredAssertionStatus()){
            logger.warn("Assertions are enabled.");            
        } else {
            logger.debug("Assertions are disabled.");
        }
        resetBoard();
        for (int i = 0; i < HISTORYSIZE; i++) {
            history[i] = new UndoableMove();
        }
    }

    private void resetBoard() {
        pinsWhite = 0;
        pinsBlack = 0;
        zobrist.reset();
        fullMoves = 0;
        occupied = 0;
        pieces = new long[2][7];
        sideToMove = Constants.WHITE;
        squares = new int[64];
        attacked = new long[64];
        attacking = new long[64];
        kings = new int[2];
        epSquare = -1;
        castleMask = 15;
        pinsBlack = 0;
        pinsWhite = 0;
        ply = 0;
        quiet50 = 0;
        materialScore = new int[2];
        pieceCount = 0;

    }

    public void doSingleMove(int move) {
        if (sideToMove == Constants.BLACK) {
            fullMoves++;
        }
        doMove(move);
    }

    public void undoSingleMove() {
        if (ply > 0) {
            undoMove();
            if (sideToMove == Constants.BLACK)
                fullMoves--;
        } else {
            logger.warn("cannot undo move: ply=0");
        }
    }

    public void undoNullMove() {
        sideToMove ^= 1;
        ply--;
        epSquare = history[ply].epSquare;
        zobrist.setHash(history[ply].hashKey);
    }

    public void doNullMove() {
        history[ply].epSquare = epSquare;
        history[ply].hashKey = zobrist.getHash();
        if (epSquare != -1) {
            zobrist.toggleSpecial(epSquare);
        }
        epSquare = -1;
        sideToMove ^= 1;
        zobrist.toggleSideToMove();
        ply++;
        assert zobrist.equalsHash(calcHashKey()) : "hashcheck failed after a null-move";
    }

    public void doMove(int move) {

        if (move < 0) {
            throw new IllegalArgumentException("Move representation cannot be smaller than 0");
        }
        int fromSquare = MV.getFromSquare(move);
        int toSquare = MV.getToSquare(move);
        long moveBitboard = 1L << fromSquare | 1L << toSquare;
        int movingPiece = squares[fromSquare];
        if (movingPiece == Constants.KING) {
            kings[sideToMove] = toSquare;
        }
        int capturedPiece = squares[toSquare];
        occupied ^= moveBitboard;
        pieces[sideToMove][Constants.ALL] ^= moveBitboard;
        pieces[sideToMove][movingPiece] ^= moveBitboard;
        squares[toSquare] = movingPiece;
        squares[fromSquare] = Constants.EMPTY;
        history[ply].epSquare = epSquare;
        history[ply].hashKey = zobrist.getHash();
        history[ply].pinsWhite = pinsWhite;
        history[ply].pinsBlack = pinsBlack;
        for (int i = 0; i < 64; i++) {
            history[ply].attacked[i] = attacked[i];
            history[ply].attacking[i] = attacking[i];
        }

        hashtory[ply] = zobrist.getHash();

        zobrist.doMove(sideToMove, movingPiece, fromSquare, toSquare, capturedPiece);

        if (epSquare != -1) {
            zobrist.toggleSpecial(epSquare);
        }
        // Deal with special moves.
        int promotedPiece = 0;
        int capturedEP = 0;
        if (MV.getSpecial(move)) {
            promotedPiece = MV.getPromotion(move);
            if (promotedPiece != 0) {
                materialScore[sideToMove] -= pieceValue[Constants.PAWN];
                materialScore[sideToMove] += pieceValue[promotedPiece];
                long promotedBitboard = 1L << toSquare;
                pieces[sideToMove][promotedPiece] ^= promotedBitboard; // old sideToMove!
                pieces[sideToMove][Constants.PAWN] ^= promotedBitboard; // old sideToMove!
                zobrist.completePromotion(sideToMove, promotedPiece, toSquare);
                squares[toSquare] = promotedPiece;
                epSquare = -1;
            } else if ((toSquare & 7) == (fromSquare & 7)) { // double move of pawn: set EP
                epSquare = toSquare;
                zobrist.toggleSpecial(epSquare);
            } else if ((toSquare & 7) == (fromSquare & 7) - 2) {
                long castleBitBoard = 1L << (fromSquare - 1) | 1L << (toSquare - 2);
                pieces[sideToMove][Constants.ALL] ^= castleBitBoard;
                pieces[sideToMove][Constants.ROOK] ^= castleBitBoard;
                occupied ^= castleBitBoard;
                squares[fromSquare - 1] = Constants.ROOK;
                squares[toSquare - 2] = Constants.EMPTY;
                epSquare = -1;
                zobrist.completeCastle(sideToMove, fromSquare - 1, toSquare - 2);
                updateAttackTables(fromSquare - 1, Constants.ROOK, sideToMove);
                updateAttackTables(toSquare - 2, Constants.EMPTY, sideToMove);
            } else if ((toSquare & 7) == (fromSquare & 7) + 2) {
                long castleBitBoard = 1L << (fromSquare + 1) | 1L << (toSquare + 1);
                pieces[sideToMove][Constants.ALL] ^= castleBitBoard;
                pieces[sideToMove][Constants.ROOK] ^= castleBitBoard;
                occupied ^= castleBitBoard;
                squares[fromSquare + 1] = Constants.ROOK;
                squares[toSquare + 1] = Constants.EMPTY;
                epSquare = -1;
                zobrist.completeCastle(sideToMove, fromSquare + 1, toSquare + 1);
                updateAttackTables(fromSquare + 1, Constants.ROOK, sideToMove);
                updateAttackTables(toSquare + 1, Constants.EMPTY, sideToMove);
            } else { // ep in action
                capturedEP = epSquare;
                squares[epSquare] = Constants.EMPTY;
                zobrist.completeEnPassant(sideToMove, epSquare);
                long epSquareBitboard = 1L << epSquare;
                pieces[sideToMove ^ 1][Constants.ALL] ^= epSquareBitboard;
                pieces[sideToMove ^ 1][Constants.PAWN] ^= epSquareBitboard;
                occupied ^= epSquareBitboard;
                epSquare = -1;
            }
        } else {
            epSquare = -1;
        }

        sideToMove ^= 1;
        zobrist.toggleSideToMove();

        if (capturedPiece != 0) {
            pieceCount--;
            materialScore[sideToMove] -= pieceValue[capturedPiece];
            long capturedBitboard = 1L << toSquare;
            occupied ^= capturedBitboard;
            pieces[sideToMove][capturedPiece] ^= capturedBitboard; // new
                                                                   // sideToMove!
            pieces[sideToMove][Constants.ALL] ^= capturedBitboard; // new
                                                                   // sideToMove!
        }

        move = MV.setCaptured(move, capturedPiece);

        history[ply].castleMask = castleMask;
        castleMask &= BB.castleMasks[fromSquare];
        castleMask &= BB.castleMasks[toSquare];
        if (castleMask != history[ply].castleMask) {
            zobrist.toggleSpecial(castleMask);
            zobrist.toggleSpecial(history[ply].castleMask);
        }
        history[ply].move = move;
        history[ply].quiet50 = quiet50;
        if (capturedPiece != Constants.EMPTY || movingPiece == Constants.PAWN) {
            quiet50 = 0;
        } else {
            quiet50++;
        }

        ply++;

        // Update attacktables
        // Remember what pieces attacked the moving piece was attacked by / will
        // be attacked by, they might be sliders and capable of moving further /
        // less now.
        // not necessary for castles
        long attackedBy;
        if (capturedEP != 0) {
            attackedBy = (attacked[fromSquare] | attacked[toSquare] | attacked[capturedEP])
                    & ~(moveBitboard | BB.single(capturedEP));
            updateAttackTables(capturedEP, Constants.EMPTY, sideToMove);
        } else if (capturedPiece == Constants.EMPTY) {
            attackedBy = (attacked[fromSquare] | attacked[toSquare]) & ~moveBitboard;
        } else {
            attackedBy = attacked[fromSquare];
            updateAttackTables(toSquare, Constants.EMPTY, sideToMove);
        }
        updateAttackTables(fromSquare, Constants.EMPTY, sideToMove ^ 1);
        if (promotedPiece == Constants.EMPTY) {
            updateAttackTables(toSquare, movingPiece, sideToMove ^ 1);
        } else {
            updateAttackTables(toSquare, promotedPiece, sideToMove ^ 1);
        }
        updateAttackTablesSecondDegree(attackedBy);
        pinsWhite = updatePins(Constants.WHITE);
        pinsBlack = updatePins(Constants.BLACK);
        assert zobrist.equalsHash(calcHashKey()) : "hashcheck failed after move";
    }

    public void undoMove() {

        if (ply == 0)
            throw new IllegalStateException("cannot undo ply 0");
        ply--;
        int move = history[ply].move;
        history[ply].move = 0;
        quiet50 = history[ply].quiet50;
        epSquare = history[ply].epSquare;
        castleMask = history[ply].castleMask;
        zobrist.setHash(history[ply].hashKey);
        pinsWhite = history[ply].pinsWhite;
        pinsBlack = history[ply].pinsBlack;
        for (int i = 0; i < 64; i++) {
            attacked[i] = history[ply].attacked[i];
            attacking[i] = history[ply].attacking[i];
        }

        int fromSquare = MV.getFromSquare(move);
        int toSquare = MV.getToSquare(move);
        int movingPiece = squares[toSquare];
        int capturedPiece = MV.getCaptured(move);
        int promotedPiece = MV.getPromotion(move);

        if (capturedPiece != 0) {
            pieceCount++;
            materialScore[sideToMove] += pieceValue[capturedPiece];
            long capturedBitboard = 1L << toSquare;
            occupied ^= capturedBitboard;
            pieces[sideToMove][capturedPiece] ^= capturedBitboard; // old sideToMove!
            pieces[sideToMove][Constants.ALL] ^= capturedBitboard; // new sideToMove!
        }

        sideToMove ^= 1;
        if (movingPiece == Constants.KING) {
            kings[sideToMove] = fromSquare;
        }

        long moveBitboard = 1L << fromSquare | 1L << toSquare;
        occupied ^= moveBitboard;
        pieces[sideToMove][Constants.ALL] ^= moveBitboard;
        pieces[sideToMove][movingPiece] ^= moveBitboard;

        squares[toSquare] = capturedPiece;
        squares[fromSquare] = movingPiece;

        if (MV.getSpecial(move)) {
            if (promotedPiece != 0) {
                materialScore[sideToMove] += pieceValue[Constants.PAWN];
                materialScore[sideToMove] -= pieceValue[promotedPiece];
                long promotedBitboard = 1L << fromSquare;
                pieces[sideToMove][promotedPiece] ^= promotedBitboard; // old
                                                                       // sideToMove!
                pieces[sideToMove][Constants.PAWN] ^= promotedBitboard; // new
                                                                        // sideToMove!
                squares[fromSquare] = Constants.PAWN;
            } else if ((toSquare & 7) == (fromSquare & 7) - 2) {
                long castleBitBoard = 1L << (fromSquare - 1) | 1L << (toSquare - 2);
                pieces[sideToMove][Constants.ALL] ^= castleBitBoard;
                pieces[sideToMove][Constants.ROOK] ^= castleBitBoard;
                occupied ^= castleBitBoard;
                squares[fromSquare - 1] = Constants.EMPTY;
                squares[toSquare - 2] = Constants.ROOK;
            } else if ((toSquare & 7) == (fromSquare & 7) + 2) {
                long castleBitBoard = 1L << (fromSquare + 1) | 1L << (toSquare + 1);
                pieces[sideToMove][Constants.ALL] ^= castleBitBoard;
                pieces[sideToMove][Constants.ROOK] ^= castleBitBoard;
                occupied ^= castleBitBoard;
                squares[fromSquare + 1] = Constants.EMPTY;
                squares[toSquare + 1] = Constants.ROOK;
            } else if ((toSquare & 7) != (fromSquare & 7)) { // ep in action (no
                                                             // double pawn
                                                             // move)
                squares[epSquare] = Constants.PAWN;
                long epSquareBitboard = 1L << epSquare;
                pieces[sideToMove ^ 1][Constants.ALL] ^= epSquareBitboard;
                pieces[sideToMove ^ 1][Constants.PAWN] ^= epSquareBitboard;
                occupied ^= epSquareBitboard;
            }
        }
    }

    private void updateAllAttackTables() {
        long whites = pieces[Constants.WHITE][Constants.ALL];
        while (whites != 0) {
            int square = BB.lsb(whites);
            whites &= whites - 1;
            updateAttackTables(square, squares[square], Constants.WHITE);
        }
        long blacks = pieces[Constants.BLACK][Constants.ALL];
        while (blacks != 0) {
            int square = BB.lsb(blacks);
            blacks &= blacks - 1;
            updateAttackTables(square, squares[square], Constants.BLACK);
        }
    }

    private void updateAttackTables(int square, int piece, int color) {
        long attackingSquares;
        long single = BB.single(square);

        // Determine new attacked and attacking bitboards
        switch (piece) {
        case Constants.QUEEN:
            attackingSquares = BB.getBishopMoves(square, occupied) | BB.getRookMoves(square, occupied);
            break;
        case Constants.ROOK:
            attackingSquares = BB.getRookMoves(square, occupied);
            break;
        case Constants.BISHOP:
            attackingSquares = BB.getBishopMoves(square, occupied);
            break;
        case Constants.KING:
            attackingSquares = BB.king[square];
            break;
        case Constants.KNIGHT:
            attackingSquares = BB.knight[square];
            break;
        case Constants.PAWN:
            attackingSquares = BB.pawn[color][square];
            break;
        case Constants.EMPTY:
            attackingSquares = attacking[square];
            removeSquareAttacked(attackingSquares, single);
            attacking[square] = 0;
            return;
        default:
            attackingSquares = 0;
        }
        attacking[square] = attackingSquares;
        addSquareToAttacked(attackingSquares, single);
    }

    private void removeSquareAttacked(long attackingSquares, long single) {
        while (attackingSquares != 0) {
            attacked[BB.lsb(attackingSquares)] &= ~single;
            attackingSquares &= attackingSquares - 1;
        }
    }

    private void toggleSquareToAttacked(long attackingSquares, long single) {
        while (attackingSquares != 0) {
            attacked[BB.lsb(attackingSquares)] ^= single;
            attackingSquares &= attackingSquares - 1;
        }
    }

    private void addSquareToAttacked(long attackingSquares, long single) {
        while (attackingSquares != 0) {
            attacked[BB.lsb(attackingSquares)] |= single;
            attackingSquares &= attackingSquares - 1;
        }
    }

    private void updateAttackTablesSecondDegree(long attackedBy) {
        // Update attacked and attacking bitboards of pieces attacking the moved
        // piece
        while (attackedBy != 0) {
            int attackingSquare = BB.lsb(attackedBy);
            long singleAttacker = BB.single(attackingSquare);
            long formerAttacks = attacking[attackingSquare];
            long difference;
            switch (squares[attackingSquare]) {
            case Constants.QUEEN:
                attacking[attackingSquare] = BB.getBishopMoves(attackingSquare, occupied)
                        | BB.getRookMoves(attackingSquare, occupied);
                difference = attacking[attackingSquare] ^ formerAttacks;
                break;
            case Constants.ROOK:
                attacking[attackingSquare] = BB.getRookMoves(attackingSquare, occupied);
                difference = attacking[attackingSquare] ^ formerAttacks;
                break;
            case Constants.BISHOP:
                attacking[attackingSquare] = BB.getBishopMoves(attackingSquare, occupied);
                difference = attacking[attackingSquare] ^ formerAttacks;
                break;
            default:
                difference = 0;
            }
            toggleSquareToAttacked(difference, singleAttacker);

            attackedBy &= attackedBy - 1;
        }
    }

    /**
     * calculates the pins for a side to move
     * 
     * @param stm
     *            the side to move
     * @return the pinned pieces.
     */
    private long updatePins(int stm) {
        long pins = 0;

        long kingAttackSquaresBishop = BB.getBishopMoves(kings[stm], pieces[stm ^ 1][Constants.ALL]);
        long kingAttackSquaresRook = BB.getRookMoves(kings[stm], pieces[stm ^ 1][Constants.ALL]);
        long kingAttackersBishop = kingAttackSquaresBishop
                & (pieces[stm ^ 1][Constants.BISHOP] | pieces[stm ^ 1][Constants.QUEEN]);
        long kingAttackersRook = kingAttackSquaresRook
                & (pieces[stm ^ 1][Constants.ROOK] | pieces[stm ^ 1][Constants.QUEEN]);

        while (kingAttackersBishop != 0) {
            int attacker = BB.lsb(kingAttackersBishop);
            kingAttackersBishop &= kingAttackersBishop - 1;
            long tempPins = kingAttackSquaresBishop & BB.getBishopMoves(attacker, pieces[stm][Constants.KING])
                    & pieces[stm][Constants.ALL];
            if (Long.bitCount(tempPins) == 1) {
                pins |= tempPins;
            }
        }
        while (kingAttackersRook != 0) {
            int attacker = BB.lsb(kingAttackersRook);
            kingAttackersRook &= kingAttackersRook - 1;
            long tempPins = kingAttackSquaresRook & BB.getRookMoves(attacker, pieces[stm][Constants.KING])
                    & pieces[stm][Constants.ALL];
            if (Long.bitCount(tempPins) == 1) {
                pins |= tempPins;
            }
        }
        return pins;
    }

    public boolean checkForRepetitions() {
        if (quiet50 < 8) {
            return false;
        }
        int count = 0;
        for (int n = ply - 2; n > ply - quiet50 && n >= 0; n -= 2) {
            if (zobrist.equalsHash(hashtory[n])) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkForSingleRepetitions() {
        if (quiet50 < 4) {
            return false;
        }
        int count = 0;
        for (int n = ply - 2; n > ply - quiet50 && n >= 0; n -= 2) {
            if (zobrist.equalsHash(hashtory[n])) {
                count++;
                if (count > 0)
                    return true;
            }
        }
        return false;
    }

    /**
     * Helper function setting up a piece on the board
     * 
     * @param type
     *            "king" - "pawn"
     * @param square
     *            "0-63"
     * @param color
     *            "WHITE-BLACK"
     */
    private void setupPiece(String type, int square, int color, boolean determineAttacksInPlace) {
        this.pieces[color][Constants.ALL] |= BB.single(square);
        occupied |= BB.single(square);
        zobrist.togglePiece(color, Constants.EMPTY, square);
        int piece = Piece.valueOf(type.toUpperCase()).getPieceID();
        if (piece == Constants.KING) {
            kings[color] = square;
        }
        squares[square] = piece;
        this.pieces[color][piece] |= BB.single(square);
        zobrist.togglePiece(color, piece, square);

        if (determineAttacksInPlace) {
            long attackedBy = attacked[square];
            updateAttackTables(square, piece, color);
            updateAttackTablesSecondDegree(attackedBy);
        }
        pieceCount++;
        materialScore[color] += pieceValue[piece];
    }

    //This method is given as an example.
    public void setupBoard2(String fen) throws IllegalFENException {
        resetBoard();
        String[] fenSubStrings = fen.split(" ");
        setupPosition(fenSubStrings[0], true);
        setupSideToMove(fenSubStrings[1]);
        setupCasleMask(fenSubStrings[2]);
        setupEpInfo(fenSubStrings[3]);
        setupOptionalInformation(fenSubStrings);
        pinsWhite = updatePins(Constants.WHITE);
        pinsBlack = updatePins(Constants.BLACK);
    }

    public void setupBoard(String fen) throws IllegalFENException {
        resetBoard();
        String[] fenSubStrings = fen.split(" ");
        setupPosition(fenSubStrings[0], false);
        setupSideToMove(fenSubStrings[1]);
        setupCasleMask(fenSubStrings[2]);
        setupEpInfo(fenSubStrings[3]);
        setupOptionalInformation(fenSubStrings);

        updateAllAttackTables();

        pinsWhite = updatePins(Constants.WHITE);
        pinsBlack = updatePins(Constants.BLACK);
    }

    private void setupOptionalInformation(String[] fenSubStrings) {
        if (fenSubStrings.length > 4) {
            quiet50 = Integer.parseInt(fenSubStrings[4]);
            if (fenSubStrings.length > 5) {
                fullMoves = Integer.parseInt(fenSubStrings[5]);
            }
        }
    }

    private void setupEpInfo(String epInfo) throws IllegalFENException {
        if ("-".equalsIgnoreCase(epInfo)) {
            epSquare = -1;
        } else {
            epSquare = MV.fileFromChar(epInfo.charAt(0));
            if (epSquare == -1) {
                throw new IllegalFENException("unexpected character in EP information" + epInfo);
            }
            switch (epInfo.charAt(1)) {
            case '3':
                epSquare += 24;
                break;
            case '6':
                epSquare += 32;
                break;
            default:
                throw new IllegalFENException("unexpected character in EP information" + epInfo);
            }
        }
        if (epSquare != -1) {
            zobrist.toggleSpecial(epSquare);
        }
    }

    private void setupSideToMove(String stmSubstring) throws IllegalFENException {
        if ("w".equalsIgnoreCase(stmSubstring)) {
            sideToMove = Constants.WHITE;
        } else if ("b".equalsIgnoreCase(stmSubstring)) {
            sideToMove = Constants.BLACK;
            zobrist.toggleSideToMove();
        } else {
            throw new IllegalFENException("unexpected party to move first: " + stmSubstring);
        }
    }

    private void setupPosition(String fenPosition, boolean determineAttacksInPlace) throws IllegalFENException {
        int row = 7;
        int column = 0;
        for (int i = 0; i < fenPosition.length(); i++) {
            switch (fenPosition.charAt(i)) {
            case '/':
                row--;
                column = 0;
                break;
            case 'r':
                setupPiece("rook", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'n':
                setupPiece("knight", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'b':
                setupPiece("bishop", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'q':
                setupPiece("queen", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'k':
                setupPiece("king", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'p':
                setupPiece("pawn", column + 8 * row, Constants.BLACK, determineAttacksInPlace);
                column++;
                break;
            case 'R':
                setupPiece("rook", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case 'N':
                setupPiece("knight", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case 'B':
                setupPiece("bishop", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case 'Q':
                setupPiece("queen", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case 'K':
                setupPiece("king", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case 'P':
                setupPiece("pawn", column + 8 * row, Constants.WHITE, determineAttacksInPlace);
                column++;
                break;
            case '1':
                column++;
                break;
            case '2':
                column = column + 2;
                break;
            case '3':
                column = column + 3;
                break;
            case '4':
                column = column + 4;
                break;
            case '5':
                column = column + 5;
                break;
            case '6':
                column = column + 6;
                break;
            case '7':
                column = column + 7;
                break;
            case '8':
                column = column + 8;
                break;
            default:
                throw new IllegalFENException("unexpected character in FEN position: " + fenPosition);
            }
        }
    }

    private void setupCasleMask(String castleInfo) throws IllegalFENException {
        castleMask = 0;
        if (!"-".equalsIgnoreCase(castleInfo)) {
            for (int i = 0; i < castleInfo.length(); i++) {
                int color;
                int maskBit;
                String kingSquare;
                String rookSquare;
                switch (castleInfo.charAt(i)) {
                case 'K':
                    color = Constants.WHITE;
                    maskBit = 2;
                    kingSquare = "e1";
                    rookSquare = "h1";
                    break;
                case 'Q':
                    color = Constants.WHITE;
                    maskBit = 1;
                    kingSquare = "e1";
                    rookSquare = "a1";
                    break;
                case 'k':
                    color = Constants.BLACK;
                    maskBit = 8;
                    kingSquare = "e8";
                    rookSquare = "h8";
                    break;
                case 'q':
                    color = Constants.BLACK;
                    maskBit = 4;
                    kingSquare = "e8";
                    rookSquare = "a8";
                    break;
                default:
                    throw new IllegalFENException("unexpected character in Castle information: " + castleInfo);
                }
                if (confirm(color, Constants.KING, kingSquare)
                        && confirm(color, Constants.ROOK, rookSquare)) {
                    castleMask |= maskBit;
                }
            }
        }
        zobrist.toggleSpecial(castleMask);
    }

    private boolean confirm(int color, int type, String squareString) {
        int square = MV.toBasicSquare(squareString);
        return (pieces[color][type] & BB.single(square)) != 0;
    }

    /**
     * Calculates the hashkey of a position from scratch!
     * 
     * @return clean, calculated hashKey
     */
    private long calcHashKey() {
        ctrlZobrist.reset();
        for (int i = 0; i < 64; i++) {
            if (squares[i] != Constants.EMPTY) {
                if ((BB.single(i) & pieces[Constants.WHITE][Constants.ALL]) != 0) {
                    ctrlZobrist.togglePiece(Constants.WHITE, squares[i], i);
                    ctrlZobrist.togglePiece(Constants.WHITE, Constants.EMPTY, i);
                } else {
                    ctrlZobrist.togglePiece(Constants.BLACK, squares[i], i);
                    ctrlZobrist.togglePiece(Constants.BLACK, Constants.EMPTY, i);
                }
            }
        }
        if (epSquare != -1) {
            ctrlZobrist.toggleSpecial(epSquare);
        }
        ctrlZobrist.toggleSpecial(castleMask);
        if (Constants.BLACK == sideToMove) {
            ctrlZobrist.toggleSideToMove();
        }
        return ctrlZobrist.getHash();
    }

    public boolean isInTableBaseRange() {
        return Syzygy.isAvailable(Long.bitCount(occupied));
    }

    public long getPins(int stm) {
        if (stm == Constants.WHITE)
            return pinsWhite;
        else
            return pinsBlack;
    }

    public int getMaterialScore(int stm) {
        return materialScore[stm];
    }

    public int getPieceCount(){
        return pieceCount;
    }

    public long[] getAttacking() {
        return attacking;
    }

    public long[] getAttacked() {
        return attacked;
    }

    public int getKings(int stm) {
        return kings[stm];
    }

    public long[][] getPieces() {
        return pieces;
    }

    public int[] getSquares() {
        return squares;
    }

    public int getEpSquare() {
        return epSquare;
    }

    public int getCastleMask() {
        return castleMask;
    }

    public int getSideToMove() {
        return sideToMove;
    }

    public int getQuiet50() {
        return quiet50;
    }

    public int getFullMoves() {
        return fullMoves;
    }

    public long getOccupied() {
        return occupied;
    }

    public long getHashKey() {
        return zobrist.getHash();
    }

    public int getPlyCount() {
        return ply;
    }


}
