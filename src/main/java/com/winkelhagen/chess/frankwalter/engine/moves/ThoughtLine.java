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

import java.io.Serializable;

import com.winkelhagen.chess.frankwalter.engine.ScoredMove;
import com.winkelhagen.chess.frankwalter.util.MV;

public class ThoughtLine implements Serializable {
	
	private static final long serialVersionUID = -285626106017372818L;

	private int depth;
	private long duration;
	private int score;
	private int totalNodeCount;
	private String move;
	private String pv;

	public ThoughtLine(int score, int move){
		this(0, 0, score, 0, MV.toString(move), MV.toString(move));
	}

	public ThoughtLine(int depth, long duration, int totalNodeCount, ScoredMove move, int[] principalVariation, FailHighLow fail){
		this(depth, duration, move.getScore(), totalNodeCount, MV.toString(move.getMove()), MV.toString(principalVariation, fail));
	}

	private ThoughtLine(int depth, long duration, int score, int totalNodeCount, String move, String pv) {
		this.depth = depth;
		this.duration = duration;
		this.score = score;
		this.totalNodeCount = totalNodeCount;
		this.move = move;
		this.pv = pv;
	}

	public int getDepth() {
		return depth;
	}

	public long getDuration() {
		return duration;
	}

	public int getScore() {
		return score;
	}

	public int getTotalNodeCount() {
		return totalNodeCount;
	}

	public String getMove() {
		return move;
	}

	public String getPV() {
		return pv;
	}
	
	/**
	 * uses XBoard format.
	 */
	@Override
	public String toString(){
		return String.format("%d %d %d %d %s", depth, score, duration/10, totalNodeCount, pv);		
	}

	public void setScore(int score) {
		this.score = score;
	}
	//TODO: score when mating should be +/- (100000 + moves to mate)
	//Mate scores should be indicated as 100000 + N for "mate in N moves", and -100000 - N for "mated in N moves".
}
