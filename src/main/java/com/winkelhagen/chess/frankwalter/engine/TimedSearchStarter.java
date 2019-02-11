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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl.SYNC_OBJECT;


public class TimedSearchStarter implements Runnable {

    private static final Logger logger = LogManager.getLogger();
    private Engine engine;
    private long sleepToTime;
    private volatile boolean ponder;

    private final Thread waker;
    private volatile boolean ponderAllowed = false;


    /**
     * sets the remaining time to think for a running search.
     *
     * @param thinkingTime the time at to remain searching
     */
    public void setTimeRemaining(long thinkingTime) {
        this.sleepToTime = (System.currentTimeMillis() + thinkingTime);
        logger.debug("Need to make a move within {} ms now.", thinkingTime);

        engine.showLastThoughtLine();
        stopPondering();
    }

    private void stopPondering() {
        this.ponder = false;
        synchronized (SYNC_OBJECT) {
            SYNC_OBJECT.notifyAll();
        }
    }


    /**
     * Return the best possible move within the set thinking time, set statistics.
     * @param ponder are we pondering or not
     * @param thinkingTime the time allowed to think
     * @param avoidMoves moves to avoid - these are not investigated unless no other moves are available.
     *
     * @return an int representing the best move. ({@link Engine#getBestMove(Set)}
     */
    public int getBestMove(boolean ponder, long thinkingTime, Set<Integer> avoidMoves) {
        if (ponder && !ponderAllowed){
            logger.debug("ponder not allowed now");
            return 0;
        }
        long systemTime = System.currentTimeMillis();
        if (ponder && sleepToTime > systemTime){
            logger.debug("already a ponder-hit, not pondering");
            ponder = false;
            thinkingTime = sleepToTime - systemTime;
        }

        logger.debug("Need to make a move within {} ms.", thinkingTime);

        // Set an alarm
        if (!ponder) {
            sleepToTime = (systemTime + thinkingTime);
        }
        this.ponder = ponder;

        // Find the actual move (this also causes the thread to stop waiting, if ponder is false)
        int move = engine.getBestMove(avoidMoves);

        waker.interrupt(); //might be a forced move, so waker might be sleeping.
        return move;
    }

    /**
     * @param engine - the engine to use
     */
    public TimedSearchStarter(Engine engine) {
        this.engine = engine;
        waker = new Thread(this, "waker-bm");
        waker.setPriority(7);
        waker.setDaemon(true);
        waker.start();
    }

    @Override
    public void run() {
        while (true) { //NOSONAR
            try {
                //first, wait until the engine is running, if needed.
                synchronized (SYNC_OBJECT) {
                    while (!engine.isRunning() || (ponderAllowed && ponder)) {
                        logger.debug("engine is not running or we're in ponder: waiting");
                        SYNC_OBJECT.wait();
                        logger.debug("engine was not running: notified");
                    }
                    if (ponder && !ponderAllowed) {
                        logger.debug("pondering was requested, but not allowed");
                    }
                }
                long sleepTime = (sleepToTime - System.currentTimeMillis());
                if (sleepTime > 0) {
                    logger.debug("stopping engine in {} ms", sleepTime);
                    Thread.sleep(sleepTime);
                } else {
                    logger.debug("not sleeping because sleepTime ({}) is zero or negative", sleepTime);
                }
                engine.forceStop();
            } catch (InterruptedException ie) { //NOSONAR
                logger.debug("interrupted - not stopping engine");
            }
        }
    }

    public void setPonderAllowed(boolean ponderAllowed) {
        this.ponderAllowed = ponderAllowed;
    }
}
