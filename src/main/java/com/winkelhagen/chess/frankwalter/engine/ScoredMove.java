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

/**
 * convenience class to hold sortable (on scoring) moves.
 * Only used at the root node, due to angst of constructors.
 *
 */
public class ScoredMove implements Comparable<ScoredMove> {

    private int score;
    private int move;
    private int depth;


    /**
     * Constructs a ScoredMove with score = 0 and depth = 0 around a move.
     * @param move the move
     */
    public ScoredMove(int move){
        this.move = move;
        score = 0;
        depth = 0;
    }

    /**
     * Compares one ScoredMove to another. A move is better if the score is higher with the depth factored in (deeper = better)
     * An EXACT_SCORE is always better than an ALPHA_SCORE.
     * @param anotherMove another ScoredMove
     */
    public int compareTo(ScoredMove anotherMove) {
        int resultingDepth = anotherMove.getDepth() - depth; // negative is better
        if (resultingDepth!=0) return resultingDepth;
        return anotherMove.getScore() - score;
    }
    /**
     * @param score the score to set
     */
    public void setScore(int score) {
        this.score = score;
    }
    /**
     * @return the score
     */
    public int getScore() {
        return score;
    }
    /**
     * @param move the move to set
     */
    public void setMove(int move) {
        this.move = move;
    }
    /**
     * @return the move
     */
    public int getMove() {
        return move;
    }
    /**
     * @param depth the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }
    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoredMove that = (ScoredMove) o;

        return score == that.score && move == that.move && depth == that.depth;
    }

    @Override
    public int hashCode() {
        int result = score;
        result = 31 * result + move;
        result = 31 * result + depth;
        return result;
    }
}
