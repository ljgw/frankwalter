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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.engine.moves.ThoughtLine;

/**
 * Class holding some numbers describing the statistics of the search.
 */
public class SearchStatistics {

    private static final Logger LOG = LogManager.getLogger();

    int nodecount;
    int betacut;
    int qnodecount;
    int qbetacut;
    int tthits;
    int ttfails;
    long starttime = System.currentTimeMillis();
    int iddcount;
    int nonull;
    int onenull;
    int morenull;
    int tbhits;
    List<ThoughtLine> thoughtLines = new ArrayList<>();

    String checkForMate = null;

    int nullMoves;
    int nullMoveTries;


    public List<ThoughtLine> getThoughtLines() {
        return thoughtLines;
    }

    /**
     * stop statistics and log them
     * 
     * @param move the move that is played.
     */
    public void stop(String move) {
        long duration = new GregorianCalendar().getTimeInMillis() - starttime;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found move {} in {} millis.", move, duration);
            LOG.debug("Nodecount: {}: (with {} betacuts).", nodecount, betacut);
            LOG.debug("Quietnodes: {} with {} betacuts.", qnodecount, qbetacut);
            LOG.debug("TranspositionTable: +{}/-{}.", tthits, ttfails);
            LOG.debug("Tablebase hits: {}",tbhits);
            LOG.debug("IDD Count: {}.", iddcount);
            LOG.debug("Nullmoves: {} {} {}. Nullmoves: {}/{}.", nonull, onenull, morenull, nullMoves, nullMoveTries);
        }
    }

}
