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
package com.winkelhagen.chess.frankwalter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by laurens on 19-12-16 for frankwalter.
 */
public class GameTimer {

    private int ownTime = 300000;
    private int otherTime = 300000;
    private int fullMovesPerSession = 40;
    private int incTime = 0;
    private boolean singleMove = false;

    /**
     * setup a fixed time for single move time control
     * @param time the time in seconds to use
     */
    public void setTimeSingleMove(double time) {
        ownTime = (int)(time * 1000);
        singleMove = true;
    }

    /**
     * helper function to parse time control instructions with the actual numbers instead of strings.
     *
     * @param fullMovesPerSession
     *            the number of moves each player should do within a time session.
     * @param time
     *            the number of seconds for each time session
     * @param inc
     *            the number of seconds the clock is increased every move.
     */
    public void parseLevel(int fullMovesPerSession, double time, double inc) {
        this.fullMovesPerSession = fullMovesPerSession;
        int baseTime = (int)(time * 1000);
        this.incTime = (int)(inc * 1000);
        ownTime = baseTime;
        otherTime = baseTime;
        singleMove = false;
    }

    /**
     * Calculate the time we dare take for a move based roughly on:
     * <ul>
     * <li>our time</li>
     * <li>number of moves we still need to do</li>
     * <li>extra time we get each move</li>
     * <li>opponents time</li>
     * </ul>
     *
     * @param fullMoves full moves played in the game
     * @param delay the number of millis execution of this command was delayed
     * @return the time we can use in milliseconds.
     */
    public int calculateTime(int fullMoves, long delay) {
        ownTime -= delay;
        if (singleMove) {
            return calculateTimeFixedForMove();
        } else if (fullMovesPerSession == 0) {
            return calculateTimeFixedForGame(fullMoves);
        } else {
            return calculateTimeForSession(fullMoves);
        }
    }

    /**
     * calculates the time for this move based on a naive calculation based on the number of moves left in the session. TODO: improve!
     *
     * @param fullMoves full moves played in the game
     * @return the time in millis that we're going to think.
     */
    private int calculateTimeForSession(int fullMoves) {
        int movesLeft = fullMovesPerSession - fullMoves;
        while (movesLeft < 1) {
            movesLeft += fullMovesPerSession;
        }

        int returnTime = (ownTime-incTime) / (movesLeft + 2) + incTime;
        if (returnTime > (ownTime - 100)) {
            return ownTime / 2;
        } else {
            return returnTime;
        }
    }

    /**
     * simple method to calculate the thinking time when this is fixed.
     * @return the time to think, minus a margin
     */
    private int calculateTimeFixedForMove() {
        return ownTime - 100;
    }

    /**
     * calculates the time for this move based on a naive calculation based on the number of moves in the game. TODO: improve!
     *
     * @param fullMoves full moves played in the game
     * @return the time in millis that we're going to think.
     */
    private int calculateTimeFixedForGame(int fullMoves) {
        int factor = 40;
        if (fullMoves > 30 && ownTime > otherTime) {
            factor = 20;
        }
        if (incTime > 100){
            return Math.min(((ownTime-incTime) / factor) + incTime-100, ownTime-100);
        } else {
            return ((ownTime-incTime) / factor);
        }
    }

    public void setOwnTime(int ownTime) {
        this.ownTime = ownTime * 10;
    }

    public void setOtherTime(int otherTime) {
        this.otherTime = otherTime * 10;
    }

}
