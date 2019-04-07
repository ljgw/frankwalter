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
package com.winkelhagen.chess.frankwalter.tuner;

import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.engine.Engine;
import com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl;
import com.winkelhagen.chess.frankwalter.engine.evaluator.Evaluator;
import com.winkelhagen.chess.frankwalter.util.Constants;

public class Environment {

    private Engine engine = new ScoutEngineImpl();
    private Board board = new Board();

    public Environment(){
        engine.setBoard(board);
    }

    public void setupBoard(String fen) throws IllegalFENException {
        board.setupBoard2(fen);
    }

    public int getQScore(){
//        engine.clearCaches();
        int qScore = engine.getQScore();
        return board.getSideToMove() == Constants.WHITE? qScore: -qScore;
    }

    public int eval(){
        int eval = Evaluator.eval(board, -32000, 32000);
        return board.getSideToMove() == Constants.WHITE? eval: -eval;
    }

}