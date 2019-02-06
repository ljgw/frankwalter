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
 * Enumeration of piece types, with associated pieceChars (for SAN notation) and pieceStrings (for display of the board)
 */
public enum Piece {
	QUEEN(Constants.QUEEN, 'Q', "q"), KING(Constants.KING, 'K', "k"), ROOK(Constants.ROOK, 'R', "r"), KNIGHT(Constants.KNIGHT, 'N', "n"), BISHOP(Constants.BISHOP, 'B', "b"), PAWN(Constants.PAWN, null, "p");
	
	private int pieceID;
	private Character pieceChar;
	private String pieceString;

	Piece(int pieceID, Character pieceChar, String pieceString){
		this.pieceID = pieceID;
		this.pieceChar = pieceChar;
		this.pieceString = pieceString;
	}

	public int getPieceID() {
		return pieceID;
	}

	/**
	 * Determine which piece is being moved (first position) / promoted into (first position after '=')
	 * @param pieceChar the character that might denote a piece
	 * @return the appropriate piece for 'K', 'Q', 'R', 'B' and 'N' - PAWN for any other character
	 */
	public static int getPieceIDFromSAN(Character pieceChar){
	    for (Piece piece : values()){
	        if (piece.pieceChar!=null && piece.pieceChar.equals(pieceChar)){
	            return piece.pieceID;
	        }
	    }
	    return PAWN.pieceID;
	}

    public static String getCharFromPieceID(int id) {
        for (Piece piece : values()){
            if (piece.pieceID==id){
                return piece.pieceString;
            }
        }
        return "";
    }
}
