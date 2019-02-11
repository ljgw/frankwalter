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
package com.winkelhagen.chess.frankwalter.engine;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable;

import java.util.Set;

/**
 * interface for engines - allows us to switch engines with ease.
 * 
 * @author Lau
 *
 */
public interface Engine {

    /**
     * Return the best possible move, set statistics. Think infinitely
     * @param avoidMoves moves to avoid - these are not investigated unless no other moves are available.
     *
     * @return an int representing the best move.
     */
    int getBestMove(Set<Integer> avoidMoves);

    /**
     * Setter for the board.
     * 
     * @param board
     *            the board
     */
    void setBoard(Board board);

    /**
     * Sets the max depth we're going to search to.
     * 
     * @param maxDepth
     *            the max depth we're going to search to
     */
    void setMaxDepth(int maxDepth);

    /**
     * Tell the engine it should stop thinking.
     */
    void forceStop();

    /**
     * Tell the engine whether is should show it's thinking. The default is true.
     * 
     * @param showThinking
     *            show thinking, yes or no
     */
    void setShowThinking(boolean showThinking);

    /**
     * @param tt
     *            the Transposition Table to set
     */
    void setTranspositionTable(TranspositionTable tt);

    /**
     * query the board for the best move according to the TranspositionTable
     * @return the hashmove from the TranspositionTable
     */
    int getBestMoveFromTT();

    SearchStatistics getStatistics();

    /**
     * get the quiet score of this position
     * @return the score based on the q-search
     */
    int getQScore();

    boolean getShowThinking();

    void clearCaches();

    void printStatistics();

    String historyStatistics();


    /**
     * is the engine running
     * @return true iff stopEngine == false;
     */
    boolean isRunning();

    void showLastThoughtLine();
}
