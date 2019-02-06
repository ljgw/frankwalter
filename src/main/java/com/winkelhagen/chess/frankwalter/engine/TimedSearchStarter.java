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


    /**
     * sets the remaining time to think for a running search.
     *
     * @param sleepToTime the time at which we should stop thinking
     */
    public void setTimeRemaining(long sleepToTime) {
        int thinkingTime = (int) (sleepToTime - System.currentTimeMillis());
        logger.debug("Need to make a move within {} ms now.", thinkingTime);

        engine.showLastThoughtLine();
        this.sleepToTime = sleepToTime;
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
     * @param sleepToTime the time at which we should stop thinking
     * @param avoidMoves moves to avoid - these are not investigated unless no other moves are available.
     *
     * @return an int representing the best move. ({@link Engine#getBestMove(Set)}
     */
    public int getBestMove(boolean ponder, long sleepToTime, Set<Integer> avoidMoves) {
        int thinkingTime = (int) (sleepToTime - System.currentTimeMillis());
        logger.debug("Need to make a move within {} ms.", thinkingTime);

        // Set an alarm
        if (!ponder) {
            this.sleepToTime = sleepToTime;
        }
        this.ponder = ponder;

        // Find the actual move (this also causes the thread to stop waiting, if ponder is false)
        return engine.getBestMove(avoidMoves);
    }

    /**
     * @param engine - the engine to use
     */
    public TimedSearchStarter(Engine engine) {
        this.engine = engine;
        waker = new Thread(this, "waker-bm");
        waker.setDaemon(true);
        waker.start();
    }


    //todo: keep this thread alive, in a while true loop. Get rid of thread interrupt code above.
    //after the engine is stopped, wait again till the engine runs again and we're not pondering.
    @Override
    public void run() {
        while (true) {
            try {
                //first, wait until the engine is running, if needed.
                synchronized (SYNC_OBJECT) {
                    while (!engine.isRunning() || ponder) {
                        logger.debug("engine is not running or we're in ponder: waiting");
                        SYNC_OBJECT.wait();
                        logger.debug("engine was not running: notified");
                    }
                }
                long sleepTime = (sleepToTime - System.currentTimeMillis());
                if (sleepTime > 0) {
                    logger.debug("stopping engine in {} ms", sleepTime);
                    Thread.sleep(sleepTime);
                } else {
                    logger.debug("not sleeping because sleepTime ({}) is zero or negative", sleepTime);
                }
                if (Thread.currentThread().isInterrupted()) {
                    logger.debug("interrupted - not stopping engine");
                    return;
                }
                engine.forceStop();
            } catch (InterruptedException ie) {
                logger.debug("interrupted - not stopping engine");
                Thread.currentThread().interrupt();
            }
        }
    }

}
