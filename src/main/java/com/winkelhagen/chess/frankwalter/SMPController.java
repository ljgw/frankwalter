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

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl;
import com.winkelhagen.chess.frankwalter.engine.SearchStatistics;
import com.winkelhagen.chess.frankwalter.engine.tt.Entry;
import com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SMPController {
    private static final Logger logger = LogManager.getLogger();

    private List<Board> boardList = new ArrayList<>();
    private List<ScoutEngineImpl> engineList = new ArrayList<>();
    private List<AtomicInteger> searchDepths = new ArrayList<>();
    private TranspositionTable transpositionTable;
    private Integer maxSearchDepth;
    private boolean running = false;
    private boolean post = true;

    public SMPController() {
        add(1);
        engineList.get(0).setShowThinking(post);
    }

    private void remove(int n) {
        for (int i = 0; i < n; i++) {
            boardList.remove(boardList.size()-1);
            engineList.remove(engineList.size()-1);
            searchDepths.remove(searchDepths.size()-1);
            logger.debug("removed searchThread");
        }
    }

    private void add(int n) {
        int currentSize = engineList.size();
        for (int i=0; i<n; i++) {
            logger.debug("added searchThread {}", currentSize + i);
            Board board = new Board();
            ScoutEngineImpl engine = new ScoutEngineImpl();
            engine.setBoard(board);
            engine.setSearchThreadId(currentSize + i);
            if (transpositionTable!=null){
                engine.setTranspositionTable(transpositionTable);
            }
            if (maxSearchDepth!=null){
                engine.setMaxDepth(maxSearchDepth);
            }
            boardList.add(board);
            engineList.add(engine);
            searchDepths.add(new AtomicInteger(1));
        }
    }
    
    public void setTranspositionTable(TranspositionTable transpositionTable){
        this.transpositionTable = transpositionTable;
        for (ScoutEngineImpl engine : engineList){
            engine.setTranspositionTable(transpositionTable);
        }
    }

    public void showLastThoughtLine() {
        engineList.get(0).showLastThoughtLine();
    }

    public int getBestMove(Set<Integer> avoidMoves) {
        running = true;
        //start additional search threads
        for (AtomicInteger searchDepth : searchDepths) {
            searchDepth.set(1);
        }
        List<FutureTask<Integer>> futures = new ArrayList<>();
        SearchStatistics statistics = new SearchStatistics();
        for (int i = 1; i<engineList.size(); i++){
            ScoutEngineImpl engine = engineList.get(i);
            FutureTask<Integer> futureTask = new FutureTask<>(() -> engine.getBestMove(avoidMoves, searchDepths, statistics));
            futures.add(futureTask);
            Thread thread = new Thread(futureTask);
            thread.setName("search-helper-" + i);
            thread.start();

        }
        int bestMove = engineList.get(0).getBestMove(avoidMoves, searchDepths, statistics);
        running = false;
        // Log some statistics
        statistics.stop(MV.toString(bestMove));
        //make sure all search threads terminate
        engineList.forEach(ScoutEngineImpl::forceStop);
        for (FutureTask<Integer> futureTask : futures){
            try {
                futureTask.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.warn("unexpected exception", e);
                Thread.currentThread().interrupt();
            }
        }
        return bestMove;
    }

    public boolean isRunning() {
        return running;
    }

    public void allowStop() {
        //allowStop is a hint, so it is only sent to the master engine
        engineList.get(0).allowStop();
    }

    public void forceStop() {
        running = false;
        //forceStop is mandatory, so it is sent to all engines
        engineList.forEach(ScoutEngineImpl::forceStop);
    }

    public Board getBoard() {
        return boardList.get(0);
    }

    public void doSingleMove(int legalMove) {
        boardList.forEach(board -> board.doSingleMove(legalMove));
    }

    public void undoSingleMove() {
        boardList.forEach(Board::undoSingleMove);
    }

    public void setupBoard(String position) throws IllegalFENException{
        for (Board board: boardList){
            board.setupBoard(position);
        }
    }

    public void setMaxDepth(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
        engineList.forEach(engine -> engine.setMaxDepth(maxSearchDepth));

    }

    public void clearCaches() {
        engineList.forEach(ScoutEngineImpl::clearCaches);
    }

    public int getBestMoveFromTT() {
        if (transpositionTable==null){
            return -1;
        }
        long ttEntry = transpositionTable.getEntry(getBoard().getHashKey());
        if (ttEntry != 0) {
            return Entry._move(ttEntry);
        } else {
            return -1;
        }

    }

    public boolean getShowThinking() {
        return engineList.get(0).getShowThinking();
    }

    public void printStatistics() {
        engineList.forEach(ScoutEngineImpl::printStatistics);
    }

    public void setCores(int cores) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (cores < 1 || cores > availableProcessors) {
            logger.warn("illegal number of cores requested '{}' - this should be a number between 1 and {}", cores, availableProcessors);
            return;
        }
        if (cores>engineList.size()){
            add(cores - engineList.size());
        } else if (cores<engineList.size()) {
            remove(engineList.size() - cores);
        }
    }

    public void setPost(boolean post) {
        this.post = post;
        engineList.get(0).setShowThinking(post);
    }
}
