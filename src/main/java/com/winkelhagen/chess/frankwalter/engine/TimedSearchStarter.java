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

import com.winkelhagen.chess.frankwalter.SMPController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl.SYNC_OBJECT;


public class TimedSearchStarter implements Runnable {

    private static final Logger logger = LogManager.getLogger();
    private SMPController controller;
    private long sleepToTime;
    private long sleepToTimeOptimistic;
    private volatile boolean ponder;
    private volatile boolean ponderDisallowed = false;
    private volatile boolean ponderHit = false;

    private final Thread waker;
    private boolean useStrictTime = false;


    public void disallowPonder(boolean ponderHit) {
        if (ponderHit){
            controller.showLastThoughtLine();
            this.ponderHit = true;
        }
        ponderDisallowed = true;
        stopPondering();
    }

    public void stopPondering(){
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
     * @return an int representing the best move. ({@link Engine#getBestMove(Set, java.util.List, SearchStatistics)}
     */
    public int getBestMove(boolean ponder, long thinkingTime, Set<Integer> avoidMoves) {
        this.ponder = ponder;
        long systemTime = System.currentTimeMillis();
        if (this.ponder && ponderDisallowed){
            logger.debug("ponder, not pondering");
            if (ponderHit){
                //ponder hit, so do think from this position
                this.ponder = false;
            } else {
                ponderDisallowed = false;
                return 0;
            }
        }

        logger.debug("Need to make a move within {} ms.", thinkingTime);

        // Set an alarm
        if (ponder) {
            sleepToTimeOptimistic = (systemTime + thinkingTime);
            sleepToTime = (systemTime + thinkingTime);
        } else {
            sleepToTimeOptimistic = (systemTime + (thinkingTime / 2));
            sleepToTime = (systemTime + thinkingTime);
        }

        // Find the actual move (this also causes the thread to stop waiting, if ponder is false)
        int move = controller.getBestMove(avoidMoves);
        ponderDisallowed = false;
        ponderHit = false;

        waker.interrupt(); //might be a forced move, so waker might be sleeping.
        return move;
    }

    /**
     * @param controller - the controller to use
     */
    public TimedSearchStarter(SMPController controller) {
        this.controller = controller;
        waker = new Thread(this, "waker-bm");
        waker.setPriority(7);
        waker.setDaemon(true);
        waker.start();
    }

    @Override
    public void run() {
        try {
            runLoop();
        } catch (RuntimeException re){
            logger.error("unexpected runtime exception", re);
        }
    }

    private void runLoop() {
        while (true) { //NOSONAR
            long ponderTime = 0;
            try {
                synchronized (SYNC_OBJECT) {
                    //first, wait until the engine is running, if needed.
                    while (!controller.isRunning()) {
                        logger.debug("engine is not running: waiting");
                        SYNC_OBJECT.wait();
                        logger.debug("engine is running");
                    }
                    //if we ponder, also wait.
                    while (ponder) {
                        logger.debug("we're in ponder: waiting");
                        long ponderTimeStart = System.currentTimeMillis();
                        SYNC_OBJECT.wait();
                        ponderTime += (System.currentTimeMillis() - ponderTimeStart);
                        logger.debug("ponder is done");
                    }
                }
                //if not using a fixed time per move, we want to not waste time thinking about moves that are certain to be played.
                if (!useStrictTime) {
                    long sleepTime = (sleepToTimeOptimistic - System.currentTimeMillis());
                    if (sleepTime > 0) {
                        logger.debug("allowing engine to stop in {} ms", sleepTime);
                        Thread.sleep(sleepTime);
                    } else {
                        logger.debug("allowing engine to stop because sleepTime ({}) is zero or negative", sleepTime);
                    }
                    controller.allowStop();
                }
                //todo: increase this non-optimistic sleepToTime based on the time spent pondering.
                long sleepTime = (sleepToTime - System.currentTimeMillis()) + ponderTime;
                if (sleepTime > 0) {
                    logger.debug("stopping engine in {} ms (pondertime was {})", sleepTime, ponderTime);
                    Thread.sleep(sleepTime);
                } else {
                    logger.debug("stopping engine because sleepTime ({}, ponderTime was {}) is zero or negative", sleepTime, ponderTime/2);
                }
                controller.forceStop();
            } catch (InterruptedException ie) { //NOSONAR
                logger.debug("interrupted - not stopping engine");
            }
        }
    }

    public void setUseStrictTime(boolean useStrictTime) {
        this.useStrictTime = useStrictTime;
    }
}
