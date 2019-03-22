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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.winkelhagen.chess.frankwalter.util.Square;
import com.winkelhagen.chess.frankwalter.engine.evaluator.Seer;
import com.winkelhagen.chess.frankwalter.engine.moves.FailHighLow;
import com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator;
import com.winkelhagen.chess.frankwalter.engine.tb.Syzygy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;
import com.winkelhagen.chess.frankwalter.engine.evaluator.Evaluator;
import com.winkelhagen.chess.frankwalter.engine.moves.ThoughtLine;
import com.winkelhagen.chess.frankwalter.engine.tt.Entry;
import com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTable;
import com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTableDummy;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.MV;

/**
 * Implementation of the Alpha-Beta search algorithm.
 */
public class ScoutEngineImpl implements Engine {

    private static final Logger logger = LogManager.getLogger();

    private static final int[][] LMR_TABLE = new int[64][32];
    static {
        // Ethereal LMR formula with depth and number of performed moves // tweaked from chess22k
        for (int depth = 1; depth < 65; depth++) {
            for (int moveNumber = 1; moveNumber < 32; moveNumber++) {
                LMR_TABLE[depth-1][moveNumber] = Math.max(0, (int)(2 + 8 * Math.log(depth) * Math.log(moveNumber * 1.2f) / 2.5f));
            }
        }
    }
    /*
     * Absolute max depth to search a position is set to 100, which we only reach in super end-game.
     * To keep the movesTable small, we use a predicted max moves number as the maximum number of moves in
     * a position. Apparently the absolute max is 218 (in: R6R/3Q4/1Q4Q1/4Q3/2Q4Q/Q4Q2/pp1Q4/kBNN1KB1 w - - 0 1) --
     * This, however, is defined in the Board class.
     */
    public static final int ABSOLUTE_MAX_DEPTH = 125;
    public static final int MAX_DEPTH_MARGIN = 50;

    /*
     * score related constants: (INFINITY) Impossible max scores (alpha = -INFINITY, beta = INFINITY) to give bounds to
     * searchwindows. (MATED) Theoretical score for a MATED position (with each ply approaching MATED is one closer to
     * zero). (MATED_IN_X) Upper bound for positions that will result in being MATED. (MATED_IN_Q) Score for positions
     * that will result in being MATED as found in Quiescence Search.
     */
    private static final int INFINITY = 32000;
    private static final int MATED = -31000;
    private static final int MATE_IN_X = -30000;
    private static final int MATE_IN_Q = -29000;
    public static final int MATE_TT = -25000;

    /*
     * Window for the Aspiration Search (small bounds close to guessed score of the position)
     */
    private static final int ASPIRATION_WINDOW = 25;
    public static final Object SYNC_OBJECT = new Object();

    /*
     * the other main components of this chess program
     */
    private Board board;
    private TranspositionTable tt = new TranspositionTableDummy();

    /*
     * For reasons of performance we reuse the same memory for generated moves throughout the game. We need one array of
     * moves per ply.
     */
    private int[][] movesTable = new int[ABSOLUTE_MAX_DEPTH][Board.ABSOLUTE_MAX_MOVES];

    /*
     * Using fractional plies to simplify dynamic search depth (extensions and reductions)
     */
    public static final int ONE_PLY = 8;

    /*
     * Some constants governing search
     */
    private static final int NULL_MOVE_REDUCTION = (int) Math.round(3.0d * ONE_PLY); // make dynamic?
    private static final int NULL_MOVE_REDUCTION_2 = (int) Math.round(2.2d * ONE_PLY); // make dynamic?
    private static final int CHECK_EXTENSION = 8;

    /*
     * variables used in a search () maxDepth is the MaxDepth to search (minimum of ABSOLUTE_MAX_DEPTH-MAX_DEPTH_MARGIN (for qsearch)
     * and a user defined value) () currentDepth is the current iteration of iterative deepening. ()
     * selectiveSearchDepth is the dynamic depth to search a branch in the tree (starting at currentDepth*ONE_PLY and
     * influenced by extentions and reductions) () searchIteration is the iteration of the search (useful to keep track
     * of how thorough the search was (and accurate a score is) ()
     * killer1 and killer2 are two slots to keep killer moves at each depth for move ordering
     */
    private int maxDepth = ABSOLUTE_MAX_DEPTH - MAX_DEPTH_MARGIN;
    private int currentDepth = 0;
    private int selectiveSearchDepth = 0;
    private int searchIteration = 0;
    private int[] principalVariation;
    private int[] killer1;
    private int[] killer2;
    private int[][] history = new int[2][4096];
    private int[][] historyBetaCut = new int[2][4096];

    /*
     * flags used in the engine hardStopEngine if it becomes true: stop the engine as quickly as possible. (mate move found,
     * or a forced move) showThinking to comply with the winboard option to not show thinking mateVerification turn on for
     * mate verification researches - no null-move, no cuts from TT, no eval. usedNull flag set
     * so that we wont try two null moves in a row
     */
    private volatile boolean hardStopEngine = true;
    private volatile boolean allowStopEngine = true;
    private boolean panic = false;
    private boolean allowPanic = true;


    //always false for every engine except the first.
    private boolean showThinking = false;



    /*
     * for statistics pertaining the whole search of this position
     */
    private SearchStatistics statistics = null;

    private ThoughtLine lastThoughtLine;
    private int[] bestMove = new int[ABSOLUTE_MAX_DEPTH];
    private int[] bestScore = new int[ABSOLUTE_MAX_DEPTH];
    private int searchThreadId = 0;

    public void setSearchThreadId(int searchThreadId){
        this.searchThreadId = searchThreadId;
    }

    /**
     * Starting point of our searches. Wraps the actual search to fix errors in case of incidents such as
     *
     * () searches where positions with more than {@value Board#ABSOLUTE_MAX_MOVES} can occur.
     *
     * After the search SearchStatistic will be available (until a new search comes)
     *
     * @return the best move
     */
    @Override
    public int getBestMove(Set<Integer> avoidMoves, List<AtomicInteger> searchDepths, SearchStatistics statistics) {

        // initialize Statistics - at the real root of our tree.
        this.statistics = statistics;
        lastThoughtLine = null;
        tt.increaseAge();

        // We're thinking again!
        allowStopEngine = false;
        hardStopEngine = false;
        panic = false;
        allowPanic = true;
        synchronized (SYNC_OBJECT) {
            SYNC_OBJECT.notifyAll();
        }
        searchIteration = 0;

        // Reset PV and killers
        principalVariation = new int[12];
        killer1 = new int[ABSOLUTE_MAX_DEPTH];
        killer2 = new int[ABSOLUTE_MAX_DEPTH];
        bestMove = new int[ABSOLUTE_MAX_DEPTH];
        degradeHistory();

        // Setup the possible moves.
        List<ScoredMove> list = generateScoredMoves();
        logger.debug("searching for best move from {} - {} possible moves", list.size(), avoidMoves.size());

        if (list.isEmpty()) {
            return 0; // No move is possible.
        }

        if (!avoidMoves.isEmpty()){
            if (list.size() <= avoidMoves.size()){
                logger.warn("avoid moves list size is greater or equal to actual moves list");
            } else {
                list.removeIf((ScoredMove scoredMove) -> avoidMoves.contains(scoredMove.getMove()));
            }
        }

        // Iterative deepening until the engine is stopped or at max depth.
        if (searchThreadId==0) {
            currentDepth = 1;
        } else {
            currentDepth = 2 + (searchThreadId%3);
        }


        // While the engine is not stopped (mate / forced move / out of time) and we're not at maxDepth
        while (!isEngineAllowedToStop() && currentDepth < maxDepth) {

            logger.debug("currentDepth: {}", currentDepth);

            // Adjust searching depth
            selectiveSearchDepth = currentDepth * ONE_PLY;

            // start the search upto the above selectiveSearchDepth
            startSearchPVS(list);


            // hardStopEngine means out-of-time or forced move. In the first case we cannot be sure that the current move is
            // the head of the PV: so we don't print it.
            // also, we don't verify mate
            if (!hardStopEngine) {
                lastThoughtLine = generateThoughtLine(list);

                //Currently only when we mate. When we are mated we don't verify but search deeper. (todo??)
                int mateDepth = checkForMate(list);
                if (mateDepth >= 0){
                    if (showThinking) {
                        //show old thoughtline for comparison
                        OutputPrinter.printObjectOutput(lastThoughtLine);
                    }
                    lastThoughtLine = generateThoughtLine(list);
                    lastThoughtLine.setScore(100000 + mateDepth);
                }

                if (showThinking) {
                    OutputPrinter.printObjectOutput(lastThoughtLine);
                }
            }
            bestMove[currentDepth] = list.get(0).getMove();
            bestScore[currentDepth] = list.get(0).getScore();

            if (currentDepth>6) {
                panic = (bestScore[currentDepth - 1] - bestScore[currentDepth]) > Constants.SCORE_DROP_PANIC_THRESHOLD
                        || (bestScore[currentDepth - 2] - bestScore[currentDepth]) > Constants.SCORE_DROP_PANIC_THRESHOLD;
            }
            if (searchThreadId==0) {
                currentDepth++;
            } else {
                int baseDepth = searchDepths.get(0).get();
                if (baseDepth <= currentDepth){
                    currentDepth = baseDepth + 1 + searchThreadId%3;
                } else {
                    currentDepth++;
                    if (searchDepths.get(searchThreadId-1).get() == currentDepth){
                        currentDepth++;
                    }
                }
            }
            searchDepths.get(searchThreadId).set(currentDepth);
        }
        hardStopEngine = true;

        // set stopengine because we're done for now.
        // Return the best move.
        return list.get(0).getMove();
    }

    private boolean isEngineAllowedToStop() {
        //the engine must stop after a certain time, but only if
        // * this is allowed
        // * the bestMove is the same as the search iteration before
        // * the bestMove is clearly better than the secondbest move. (TODO)
        // * no drop in value over the last few iterations
        // * (or) the bestMove is 'easy'
        //or if there is a hard stop
        if (hardStopEngine){
            logger.debug("hardstopping engine");
            return hardStopEngine;
        }
        if (allowStopEngine){
            if (!allowPanic){
                return true;
            }
            if (panic){
                logger.debug("extending time once because of a scoredrop");
                allowPanic = false;
                return false;
            }
            return true;
        }
        return false;
    }

    private ThoughtLine generateThoughtLine(List<ScoredMove> list) {
        // After each iteration we print and collect a thoughtline
        mapTTonPV(list.get(0).getMove(), 0);
        return new ThoughtLine(currentDepth,
                System.currentTimeMillis() - statistics.starttime,
                statistics.nodecount + statistics.qnodecount, list.get(0), principalVariation, null);
    }

    private int checkForMate(List<ScoredMove> list) {
        // If we have a mate-score, we can stop thinking.
        if (!hardStopEngine && isMateMove(list.get(0))) {
            //TODO: separate search for quick mate? Maybe just think a ply deeper with mate boundary?

            mapTTonPV(list.get(0).getMove(), 0);

            int depth = -MATED - Math.abs(list.get(0).getScore());
            logger.debug("Mate pv: " + MV.toString(principalVariation, null));
            logger.debug("mate cut-off, depth = {}", (depth+1)/2);
            hardStopEngine = true;
            return (depth+1)/2;
        }
        return -1;
    }

    private void mapTTonPV(int move, int i) {
        principalVariation[i] = move;
        if (move == 0 || i == principalVariation.length-1) {
            return;
        }
        board.doMove(move);
        mapTTonPV(Entry._move(tt.getEntry(board.getHashKey())), i+1);
        board.undoMove();
    }

    /**
     * Generates ScoredMove objects for the current position
     * 
     * @return
     */
    private List<ScoredMove> generateScoredMoves() {

        // Generate moves
        int[] generatedMoves = movesTable[0];
        int generatedMovesNr = StaticMoveGenerator.generateLegalMoves(board, generatedMoves);

        // put them in a list
        List<ScoredMove> moves = new ArrayList<>();
        for (int i = 0; i < generatedMovesNr; i++) {
            moves.add(new ScoredMove(generatedMoves[i]));
        }

        return moves;
    }

    /**
     * If we find a mate, we can end this search early.
     * 
     * @param bestmove
     * @return
     */
    private boolean isMateMove(ScoredMove bestmove) {
        return bestmove.getScore() > -MATE_IN_X;
    }

    /**
     * Seek the best moves to a given depth. (PVS)
     * 
     * @param list
     *            moves to search
     * @return false when we must stop thinking
     */
    private void startSearchPVS(List<ScoredMove> list) {
        // The first few plies we get a feel for the position using regular full-window PVS.
        if (currentDepth < 3) {
            startPVS(list, -INFINITY, INFINITY);
        } else {
            // Try aspiration search with a window based on the best move so far
            int approximateScore = list.get(0).getScore();
            int talpha = approximateScore - ASPIRATION_WINDOW;
            int tbeta = approximateScore + ASPIRATION_WINDOW;
            startPVS(list, talpha, tbeta);
            //todo: do not allow aspiration results outside the window to give the best move.
            if (hardStopEngine) {
                return;
            }
            // if the new bestmove-score falls outside of the window, do a full re-search
            int tscore = list.get(0).getScore();
            if (tscore <= talpha || tscore >= tbeta) {
                logger.trace("aspiration search failed {}{ {}-{}: {}", selectiveSearchDepth, talpha, tbeta, tscore);
                startPVS(list, -INFINITY, INFINITY);
            }
        }
    }

    private void startPVS(List<ScoredMove> list, int alpha, int beta) {
        int score;
        int moveCount = 0;
        int originalAlpha = alpha;

        // searchIteration is increased to make clear that scores recorded in this call to startPVS are more accurate
        // than those recorded in previous calls
        searchIteration++;
        statistics.nodecount++;
        boolean isExact = false;

        // Loop through all moves (if we search within a window, we cut-off at a score equal or exceeding beta)
        for (ScoredMove move : list) {
            moveCount++;

            // Do Move
            board.doMove(move.getMove());

            // Search first move full, later moves only full when they exceed alpha. reason is we need a back-up move
            if (moveCount == 1) {
                score = -recurse(0, -beta, -alpha);
            } else {
                // Zero-Window search with -alpha as beta: if the score is alpha or worse, we don't need to investigate
                // it fully.
                score = -recurse(0, -alpha - 1, -alpha);
                if (score > alpha && score < beta) {// if score is more than or equal to beta: we will make a
                                                             // cut anyway.
                    score = -recurse(0, -beta, -alpha);
                }
            }
            // Undo Move
            board.undoMove();

            // if we exit a search and hardStopEngine == true: we didn't finish this search so we cannot use this score.
            if (hardStopEngine) {
                Collections.sort(list);
                return;
            }

            if (moveCount==1 && score < alpha) {
                //first search did not finish within (aspiration) window:
                //either this move isn't really good, or the window might be to narrow.

                // When we get here: we assume that the window is to narrow.
                logger.debug("first move in aspiration window [{}, {}] failed low {}. Researching with a wider window", alpha, beta, score);
                //todo: investigate why this occurs with the same values multiple times in a row (only with extreme scores)
                return;
            }

            //if any move fails lower than the original alpha, prefer the original order
            move.setScore(Math.max(score, originalAlpha-moveCount));
            move.setDepth(searchIteration);

            // if the score exceeds alpha, we can cut-off earlier in subsequent scout searches
            if (score > alpha) {
                isExact = true;
                //perhaps show new PV?
                if (moveCount != 1 && currentDepth >= 2) {
                    mapTTonPV(move.getMove(), 0);
                    lastThoughtLine = new ThoughtLine(currentDepth,
                            System.currentTimeMillis() - statistics.starttime,
                            statistics.nodecount + statistics.qnodecount, move, principalVariation, determineFailHighLow(score, alpha, beta));
                    if (showThinking) {
                        OutputPrinter.printObjectOutput(lastThoughtLine);
                    }
                }
                // if the score is equal to, or exceeds, beta we can cut-off now!
                if (score >= beta) {
                    Collections.sort(list);
                    statistics.betacut++;
                    tt.setEntry(board.getHashKey(), score, (short)selectiveSearchDepth, list.get(0).getMove(), Entry.FAIL_HIGH, 0);
                    return;
                }
                alpha = score;
            }

        }

        // Before we finish, we sort the moves based on score, only looking at the most current scores
        Collections.sort(list);
        if (isExact) {
            tt.setEntry(board.getHashKey(), alpha, (short)selectiveSearchDepth, list.get(0).getMove(), Entry.EXACT, 0);
        } else {
            tt.setEntry(board.getHashKey(), alpha, (short)selectiveSearchDepth, Constants.SAVE_BEST_FAIL_LOW?list.get(0).getMove():0, Entry.FAIL_LOW, 0);
        }

        // in case of only one move, do it.
        if (moveCount == 1) {
            logger.debug("forced move!");
            hardStopEngine = true;
        }

    }

    private FailHighLow determineFailHighLow(int score, int alpha, int beta) {
        if (score <= alpha){
            return FailHighLow.LOW;
        } else if (score >= beta){
            return FailHighLow.HIGH;
        } else {
            return null;
        }
    }


    /**
     * actual recursive search
     * 
     * @param depth
     *            the higher this is, the closer we are to a leave-node (qsearch)
     * @param alphaInput
     *            -Beta
     * @param betaInput
     *            -Alpha
     * @return the score of the position.
     */
    private int recurse(int depth, int alphaInput, int betaInput) {
        statistics.nodecount++;
        int alpha = alphaInput;
        int beta = betaInput;

        // check for 2fold? check elsewhere, rep draw might be worth directing the FW to.. (or from!!) yes!!
        if (board.checkForSingleRepetitions()) {
            return Evaluator.getContemptScore();
        }

        if (Constants.USE_TB && Syzygy.isAvailable(board.getPieceCount())){
            int result = Syzygy.probeWDL(board);
            if (result!=-1){
                statistics.tbhits++;
                return Syzygy.getWDLScore(result, depth);
            }
        }

        // To what depth do we search, do we trust the TT and do we go into quiescence search?
        short depthToSearch = (short) (selectiveSearchDepth - depth * ONE_PLY);

        // Depth to search is 0 or negative: enter quiescence
        if (depthToSearch <= 0) {
            statistics.nodecount--;
            return recurseQuiet(alpha, beta, depth);
        }

        // Search TT (for direct cut-offs, narrowed bounds and/or a hashMove)
        int hashMove = 0;
        long entry = tt.getEntry(board.getHashKey());
        if (entry != 0) {
            hashMove = Entry._move(entry);

            // TODO: idea: don't trust FAIL_LOW or FAIL_HIGH when it is equal to the previous aspiration search result
            // when not in aspiration search
            if (Entry._depth(entry) >= depthToSearch) {
                statistics.tthits++;
                byte type = Entry._type(entry);
                short entryScore = Entry._score(entry, depth);
                if (type == Entry.EXACT) {
                    return entryScore;
                } else if (type == Entry.FAIL_LOW) {
                    // alpha cannot be higher than score
                    if (entryScore <= alpha) {
                        return entryScore;
                    }
                    if (Constants.TT_NARROWS_BOUNDS && entryScore < beta) {
                        beta = entryScore;
                    }
                } else if (type == Entry.FAIL_HIGH) {
                    // beta cannot be lower than score
                    if (entryScore >= beta) {
                        return entryScore;
                    }
                    if (Constants.TT_NARROWS_BOUNDS && entryScore > alpha) {
                        alpha = entryScore;
                    }
                }
                statistics.tthits--;
            } else {
                statistics.ttfails++;
            }
        }

        boolean isPV = alpha != beta-1;
        //IID
        if (isPV && hashMove==0 && depthToSearch > (5.0d * ONE_PLY)){
            recurse(depth + 4, alpha, beta);
            long iidEntry = tt.getEntry(board.getHashKey());
            if (iidEntry != 0) {
                hashMove = Entry._move(iidEntry);
                statistics.iddcount++;
            }
        }

        int[] moves = movesTable[depth];
        int movesNr;
        int score;
        int extend = 0;

        // Are we in (avoidable?) check? Generate different moves.
        int kingAttacker = StaticMoveGenerator.getKingAttacker(board, board.getSideToMove() ^ 1);
        if (kingAttacker != Constants.NO_SQUARE) {
            extend += CHECK_EXTENSION;
            movesNr = StaticMoveGenerator.generateOutOfCheckMoves(board, kingAttacker, moves);
            if (movesNr == 0) {
                return MATED + depth;
            }
        } else {
            movesNr = StaticMoveGenerator.generateMoves(board, moves);
            // check for stalemate
            if (movesNr == 0) {
                return Evaluator.getContemptScore();
            }
            if (allowNullMove(depth)) {
                score = tryNullMove(depth, beta);
                statistics.nullMoveTries++;
                if (score >= beta) {
                    // in a fail-hard AB: score==beta
                    statistics.betacut++;
                    statistics.nullMoves++;
                    return beta; //used to be score
                }
            }

        }

        // promote hashMove
        orderMoves(board, movesNr, moves, hashMove, killer1[depth], killer2[depth]);

        // Extend search when there are few options (forced moves)
        if (movesNr == 1) {
            extend += 7;
        } else if (movesNr == 2) {
            extend += 2;
        }

        boolean isExact = false;
        int bestMoveSoFar = 0;
        int bestScoreSoFar = -INFINITY;

        // Loop through the moves
        for (int moveNumber = 0; moveNumber < movesNr; moveNumber++) {
            int move = MV.stripScore(moves[moveNumber]);
            selectiveSearchDepth += extend;
            int lmr = 0;
            board.doMove(move);

            score = alpha+1;
            if (moveNumber > 2){// && board.getSquares()[MV.getToSquare(move)] == 0) {
                lmr -= LMR_TABLE[Math.min(depth, 63)][Math.min(moveNumber, 31)];
                if (lmr<=-8) {
                    selectiveSearchDepth += lmr;
                    score = -recurse(depth + 1, -alpha - 1, -alpha);
                    selectiveSearchDepth -= lmr;
                }
            }

            //PVS with moveNumber > 0 (starting scout)
            if (score > alpha && isPV && moveNumber > 0) {
                lmr = 0;
                score = -recurse(depth + 1, -alpha - 1, -alpha);
            }
            //alpha == beta -1 (scout) or fullsearch PVS
            if (score > alpha) {
                lmr = 0;
                score = -recurse(depth + 1, -beta, -alpha);
            }
            selectiveSearchDepth -= extend;
            board.undoMove();
            int selDepth = (selectiveSearchDepth+lmr+extend)/8;
            if (board.getSquares()[MV.getToSquare(move)] == Constants.EMPTY) {
                addToHistory(board.getSideToMove(), MV.getFromTo(move), selDepth, depth);
            }

            // update alpha, check beta
            if (score > bestScoreSoFar) {
                bestMoveSoFar = moves[moveNumber] & (1<<19)-1;
                bestScoreSoFar = score;
                if (score > alpha) {
                    isExact = true;
                    if (score >= beta) {
                        // in a fail-hard AB: score==beta
                        statistics.betacut++;
                        tt.setEntry(board.getHashKey(), score, depthToSearch, bestMoveSoFar, Entry.FAIL_HIGH, depth);
                        setKiller(depth, move, selDepth);
                        return score;
                    }
                    alpha = score;
                }
            }

            // stop the engine if needed.
            if (hardStopEngine) {
                return bestScoreSoFar;
            }
        }
        if (isExact) {
            tt.setEntry(board.getHashKey(), bestScoreSoFar, depthToSearch, bestMoveSoFar, Entry.EXACT, depth);
        } else {
            tt.setEntry(board.getHashKey(), bestScoreSoFar, depthToSearch, Constants.SAVE_BEST_FAIL_LOW?bestMoveSoFar:0, Entry.FAIL_LOW, depth);
        }
        return bestScoreSoFar;

    }


    /**
     * do a null Move + a less deep zero-window search based on beta
     *
     * @param depth
     * @param beta
     * @return
     */
    private int tryNullMove(int depth, int beta) {
        int score;
        board.doNullMove();
        int r = calcNullReduction();
        selectiveSearchDepth -= r;
        score = -recurse(depth + 1, -beta, 1 - beta);
        selectiveSearchDepth += r;
        board.undoNullMove();
        return score;
    }

    /**
     * the deeper the search goes, the more we can reduce.
     *
     * @return
     */
    private int calcNullReduction() {
        if (selectiveSearchDepth > 5 * ONE_PLY)
            return NULL_MOVE_REDUCTION;
        else
            return NULL_MOVE_REDUCTION_2;
    }

    /**
     * do we allow null-moves?
     *
     * @param depth
     *            current search depth
     * @return true if we allow null-moves, false otherwise (zugzwang)
     */
    private boolean allowNullMove(int depth) { // automatically disallowed when in check.
        return Long.bitCount(board.getPieces()[board.getSideToMove()][Constants.ALL]
                & ~board.getPieces()[board.getSideToMove()][Constants.PAWN]) >= 2;
    }

    /**
     * Update killer move holders. We use two, the latest two beta-cut moves.
     *  @param depth
     * @param move
     * @param selDepth
     */
    private void setKiller(int depth, int move, int selDepth) {
        if (board.getSquares()[MV.getToSquare(move)] == Constants.EMPTY) {
            if (move != killer1[depth]) {
                killer2[depth] = killer1[depth];
                killer1[depth] = move;
            }
            addBetaCutToHistory(board.getSideToMove(), MV.getFromTo(move), selDepth, depth);
        }
    }

    /**
     * Search till we find a quiet position
     *
     * @param alphaInput
     *            Alpha
     * @param betaInput
     *            Beta
     * @return the value of the position based on quiet positions down the tree.
     */
    private int recurseQuiet(int alphaInput, int betaInput, int depth) {
        int alpha = alphaInput;
        int beta = betaInput;
        if (Constants.USE_TB && Syzygy.isAvailable(board.getPieceCount())){
            int result = Syzygy.probeWDL(board);
            if (result!=-1){
                statistics.tbhits++;
                return Syzygy.getWDLScore(result, depth);
            }
        }
        statistics.qnodecount++;
        int[] moves = movesTable[depth];
        int movesNr;
        int bestScore = -INFINITY;

        int hashMove = 0;
        if (Constants.TT_IN_QSEARCH) {
            long entry = tt.getEntry(board.getHashKey());
            if (entry != 0) {
                hashMove = Entry._move(entry);

                statistics.tthits++;
                byte type = Entry._type(entry);
                short entryScore = Entry._score(entry, depth);
                if (type == Entry.EXACT) {
                    return entryScore;
                } else if (type == Entry.FAIL_LOW) {
                    // alpha cannot be higher than score
                    if (entryScore <= alpha) {
                        return entryScore;
                    }
                    if (Constants.TT_NARROWS_BOUNDS && entryScore < beta) {
                        beta = entryScore;
                    }
                } else if (type == Entry.FAIL_HIGH) {
                    // beta cannot be lower than score
                    if (entryScore >= beta) {
                        return entryScore;
                    }
                    if (Constants.TT_NARROWS_BOUNDS && entryScore > alpha) {
                        alpha = entryScore;
                    }
                }
                statistics.tthits--;
            }
        }

        // Are we in (avoidable?) check?
        int kingAttacker = StaticMoveGenerator.getKingAttacker(board, board.getSideToMove() ^ 1);
        if (kingAttacker != Constants.NO_SQUARE) {
            movesNr = StaticMoveGenerator.generateOutOfCheckMoves(board, kingAttacker, moves);
            if (movesNr == 0) {
                return MATE_IN_Q + depth;
            }

            //todo: how do we deal with continuous checks? by not generating checks?
            orderMoves(board, movesNr, moves, hashMove, 0, 0);
            // Else check how good standing pat is and generate captures.
        } else {
            // option: introduce SideToMove bonus.

            //todo: impose restrictions on when to use lazy eval. (if it is used)
            int patScore = Evaluator.eval(board, alpha, beta);
            if (patScore >= beta) {
                statistics.qbetacut++;
                if (Constants.TT_IN_QSEARCH) {
                    tt.setEntry(board.getHashKey(), patScore, (short) 0, 0, Entry.FAIL_HIGH, depth);
                }
                return patScore;
            }
            if (patScore > alpha) {
                alpha = patScore;
            } // ?? return patScore on else seems worse.
            movesNr = StaticMoveGenerator.generateUnquiet(board, moves);
            movesNr = orderUnquietMoves(board, hashMove, movesNr, moves);
            bestScore = patScore;
        }
        // Loop through the moves; try to make a move.
        int score;
        int bestMoveSoFar = 0;
        boolean isExact = false;
        for (int i = 0; i < movesNr; i++) {
            int move = MV.stripScore(moves[i]);
            board.doMove(move);

            // if we're in check after the move, then it's not a good move. Else recurse.
            score = -recurseQuiet(-beta, -alpha, depth + 1);
            board.undoMove();

            // update alpha, check beta
            if (score > bestScore) {
                bestMoveSoFar = move;
                if (score > alpha) {
                    if (score >= beta) {
                        statistics.qbetacut++;
                        if (Constants.TT_IN_QSEARCH) {
                            tt.setEntry(board.getHashKey(), score, (short) 0, bestMoveSoFar, Entry.FAIL_HIGH, depth);
                        }
                        return score;
                    }
                    isExact = true;
                    alpha = score;
                }
                bestScore = score;
            }
        }
        if (Constants.TT_IN_QSEARCH) {
            if (isExact){
                tt.setEntry(board.getHashKey(), bestScore, (short) 0, bestMoveSoFar, Entry.EXACT, depth);
            } else{
                tt.setEntry(board.getHashKey(), bestScore, (short) 0, Constants.SAVE_BEST_FAIL_LOW?bestMoveSoFar:0, Entry.FAIL_LOW, depth);
            }
        }

        return bestScore;
    }

    public void degradeHistory() {
        for (int i = 0; i<4096; i++){
            history[Constants.WHITE][i]>>>=4;
            history[Constants.BLACK][i]>>>=4;
            historyBetaCut[Constants.WHITE][i]>>>=4;
            historyBetaCut[Constants.BLACK][i]>>>=4;
        }
    }

    public void clearHistory(){
        Arrays.fill(history[Constants.WHITE], 0);
        Arrays.fill(history[Constants.BLACK], 0);
        Arrays.fill(historyBetaCut[Constants.WHITE], 0);
        Arrays.fill(historyBetaCut[Constants.BLACK], 0);
    }

    public void addToHistory(int stm, int fromTo, int selDepth, int depth) {
        if (selDepth>2 && depth < 11) { //depth<9?
            history[stm][fromTo] += selDepth * selDepth;
        }
    }
    public void addBetaCutToHistory(int stm, int fromTo, int selDepth, int depth) {
        if (selDepth>2 && depth < 11) { //depth<9?
            historyBetaCut[stm][fromTo] += selDepth * selDepth;
        }
    }

    @Override
    public String historyStatistics() {
        StringBuilder sb = new StringBuilder(String.format("history statistics:%n"));
        int[][] distributions = new int[2][12];
        List[] historyMoves = new List[2];
        historyMoves[0] = new ArrayList();
        historyMoves[1] = new ArrayList();
        for (int i =0; i<4096; i++){
            for (int color = 0; color<2; color++){
                if (history[color][i]==0){
                    distributions[color][0]++;
                } else {
                    int bucket = 1 + (10*historyBetaCut[color][i]/history[color][i]);
                    distributions[color][bucket]++;
                    historyMoves[color].add(String.format("%03d, %d/%d %s", 100*historyBetaCut[color][i]/history[color][i], historyBetaCut[color][i], history[color][i], MV.toString(i)));
                }
            }
        }
        sb.append(String.format("history:white 0-count '%d', max '%d'%n", Arrays.stream(history[Constants.WHITE]).filter(i -> i==0).count(), Arrays.stream(history[Constants.WHITE]).max().orElse(0)));
        sb.append(String.format("betacut:white 0-count '%d', max '%d'%n", Arrays.stream(historyBetaCut[Constants.WHITE]).filter(i -> i==0).count(), Arrays.stream(historyBetaCut[Constants.WHITE]).max().orElse(0)));
        sb.append(String.format("White Distribution: %s%n", Arrays.toString(distributions[Constants.WHITE])));
//        historyMoves[Constants.WHITE].stream().sorted(Comparator.reverseOrder()).limit(64).forEach(x -> sb.append(String.format("\t%s%n", x)));
        sb.append(String.format("history:black 0-count '%d', max '%d'%n", Arrays.stream(history[Constants.BLACK]).filter(i -> i==0).count(), Arrays.stream(history[Constants.BLACK]).max().orElse(0)));
        sb.append(String.format("betacut:black 0-count '%d', max '%d'%n", Arrays.stream(historyBetaCut[Constants.BLACK]).filter(i -> i==0).count(), Arrays.stream(historyBetaCut[Constants.BLACK]).max().orElse(0)));
        sb.append(String.format("Black Distribution: %s%n", Arrays.toString(distributions[Constants.BLACK])));
//        historyMoves[Constants.BLACK].stream().sorted(Comparator.reverseOrder()).limit(64).forEach(x -> sb.append(String.format("\t%s%n", x)));
        return sb.toString();
    }


    @Override
    public void setBoard(Board board) {
        this.board = board;
    }

    @Override
    public void setTranspositionTable(TranspositionTable tt) {
        if (this.tt != null) {
            this.tt.free();
        }
        this.tt = tt;
    }

    @Override
    public void clearCaches(){
        if (this.tt != null) {
            this.tt.clear();
        }
        clearHistory();
    }

    @Override
    public void printStatistics() {
        logger.info(historyStatistics());
        logger.info("nodecout: {}", statistics.nodecount);
    }

    public SearchStatistics getStatistics() {
        return statistics;
    }

    @Override
    public int getQScore() {
        statistics = new SearchStatistics();
        return recurseQuiet(-INFINITY, +INFINITY, 0);
    }

    @Override
    public void setMaxDepth(int depth) {
        maxDepth = Math.min(depth, ABSOLUTE_MAX_DEPTH - MAX_DEPTH_MARGIN);
    }

    @Override
    public void setShowThinking(boolean showThinking) {
        this.showThinking = showThinking;
    }

    @Override
    public boolean getShowThinking(){
        return showThinking;
    }

    @Override
    public void forceStop() {
        hardStopEngine = true;
    }

    @Override
    public void allowStop() {
        allowStopEngine = true;
    }

    @Override
    public void showLastThoughtLine() {
        if (showThinking && lastThoughtLine != null){
            //show current thoughtline
            OutputPrinter.printObjectOutput(lastThoughtLine);
        }
    }

    private int[] sortMoves(int[] moves, int length) {
        int in;

        for (int out = 1; out < length; out++) // out is dividing line
        {
            int tempMove = moves[out];
            in = out; // start shifts at out
            while (in > 0 && moves[in - 1]>>19 <= tempMove>>19) // until one is bigger,
            {
                moves[in] = moves[in - 1]; // shift item right,
                --in; // go left one position
            }
            moves[in] = tempMove;
        } // end for
        return moves;

    }

    private void orderMoves(Board board, int movesNr, int[] moves, int hashMove, int killer1, int killer2) {
        for (int i = 0; i < movesNr; i++) {
            if (moves[i] == hashMove) {
                moves[i] = MV.setScore(moves[i], Evaluator.HASHSCORE);
            } else if (moves[i] == killer1) {
                moves[i] = MV.setScore(moves[i], Evaluator.KILLERSCORE);
            } else if (moves[i] == killer2) {
                moves[i] = MV.setScore(moves[i], Evaluator.KILLERSCORE - 1);
            } else {
                moves[i] = MV.setScore(moves[i], getScore(board, moves[i]));
            }
        }
        sortMoves(moves, movesNr);
    }


    private int orderUnquietMoves(Board board, int hashMove, int movesNr, int[] moves) {
        int positiveCaptures = 0;
        int score;
        for (int i = 0; i < movesNr; i++) {
            if (moves[i] == hashMove) {
                score = Evaluator.HASHSCORE;
            } else {
                score = getQuiesceScore(board, moves[i]);
            }
            if (score >= 0) {
                positiveCaptures++;
                moves[i] = MV.setScore(moves[i], score+1);
            } else {
                moves[i] = 0;
            }
        }
        sortMoves(moves, movesNr);
        return positiveCaptures;
    }

    private int getQuiesceScore(Board board, int move) {
        return Seer.see(move, board);
    }

    private int getScore(Board board, int move) {
        int fromTo = MV.getFromTo(move);
        int to = MV.getToSquare(move);
        int capture = board.getSquares()[to];
        int score = 0;
        int sideToMove = board.getSideToMove();
        if (capture != 0) { // misses en passant captures
            int see = Seer.see(move, board);
            if (see>=0) {
                score+=Evaluator.CAPTURESCORE+see;
            } else {
                score+=Evaluator.KILLERSCORE-1+see;
            }
        } else {

            int divider = history[sideToMove][fromTo];
            if (divider!=0) {
                //Relative History Heuristic (as seen in chess22k)
                score = 100 * historyBetaCut[sideToMove][fromTo] / divider; //+100?  //+2?
            } else {
                int from = MV.getFromSquare(move);
                int piece = board.getSquares()[from];
                score = Evaluator.MOVE_PCSQ[piece - 1][Square.relative(to, sideToMove)] - Evaluator.MOVE_PCSQ[piece - 1][Square.relative(from, sideToMove)];
            }


            if ((board.getPieces()[sideToMove ^ 1][Constants.ALL] & board.getAttacked()[to]) != 0) {
                score -= 10;
            }
            score+=100;
        }
        if (MV.getPromotion(move) != Constants.EMPTY) {
            if (MV.getPromotion(move) == Constants.QUEEN) {
                if (capture == 0) {
                    score = Evaluator.CAPTURESCORE + 6;
                }
            } else {
                score = 0;
            }
        }
        return score;
    }
}