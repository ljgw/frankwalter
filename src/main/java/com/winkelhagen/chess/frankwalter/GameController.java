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
import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;
import com.winkelhagen.chess.frankwalter.config.FWConfig;
import com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator;
import com.winkelhagen.chess.frankwalter.engine.moves.ThoughtLine;
import com.winkelhagen.chess.frankwalter.engine.opening.Book;
import com.winkelhagen.chess.frankwalter.engine.opening.SimpleBookImpl;
import com.winkelhagen.chess.frankwalter.engine.tb.Syzygy;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.*;

/**
 * The GameController makes sure that all thinking is done by the EngineLoop process and that a batch of commands will be executed in the correct order.
 * The EngineLoop Thinking will be interrupted by certain commands ('go', 'usermove', '?', etcetera) but not by others, who will be queued to be executed after thinking stops.
 * For example: the sequence 'time', 'otim', 'usermove' will cause 'time' and 'otim' to be queued, to be executed after 'usermove' interrupts the engine pondering.
 *
 * Another goal of the GameController class is to make sure pondering works correctly, preventing a perpetual ponder where pondering might be initiated at the same time as the 'usermove' command is received.
 */
//TODO refactor so that all commands are executed in realtime (registering ping to output after search)
    // modify a master board (which is copied for each search thread)
    // only have one thread interact with search
public class GameController implements Runnable {

    private GameTimer gameTimer = new GameTimer();
    private boolean ponder = false;
    private volatile int pondering = 0;
    private volatile boolean newlyInitializedGame = false;
    private volatile long endPonderTime;

    private enum Command {
        INIT, GO, PLAY_OTHER, CMD, SYNC, MOVE, STOP, PONDER, PING
    }

    private class QueuedCommand {
        Command command;
        long millis;
        ExecutableCommand executableCommand;
        String parameter = "";

        QueuedCommand(Command command, String parameter, ExecutableCommand executableCommand) {
            this(command, parameter);
            this.executableCommand = executableCommand;
        }

        QueuedCommand(Command command, String parameter) {
            this(command);
            this.parameter = parameter;
        }

        QueuedCommand(Command command){
            this.command = command;
            this.millis = System.currentTimeMillis();
        }
    }

    private interface ExecutableCommand {
        void execute();
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private TransferQueue<QueuedCommand> commandQueue = new LinkedTransferQueue<>();

    private volatile boolean running = true;
    private boolean tbLoaded = false;
    private volatile boolean forceMode = false;

    private Book book;

    private final FWConfig fwConfig;

    GameController(FWConfig fwConfig){
        this.fwConfig = fwConfig;
    }

    public void startEngineLoop() {
        Thread engineLoop = new Thread(this, "EngineLoop");
        engineLoop.setPriority(Thread.NORM_PRIORITY);
        engineLoop.start();
    }

    public void stopEngineLoop() {
        stopEngineAndSync();
        commandQueue.add(new QueuedCommand(Command.STOP));
    }

    @Override
    public void run() {
        try {
            runLoop();
        } catch (RuntimeException re){
            LOGGER.error("unexpected runtime exception", re);
        }
    }

    private void runLoop() {
        while (running){
            try {
                QueuedCommand command = commandQueue.take();
                LOGGER.debug("executing queued command {} {} (queued: {})", command.command, command.parameter, commandQueue.size());
                switch (command.command){
                    case STOP:
                        running = false;
                        break;
                    case INIT:
                        fwConfig.preloadStaticClasses();
                        fwConfig.setTranspositionTable();
                        fwConfig.setAdditionalCores();
                        loadOpeningBook();
                        tbLoaded = SyzygyBridge.isLibLoaded();
                        forceMode = true;
                        clearEngineState();
                        setupStartPosition();
                        newlyInitializedGame = true;
                        break;
                    case GO:
                        newlyInitializedGame = false;
                        ensureEngineStopped();
                        forceMode = false;
                        doGo(command.millis);
                        if (ponder){
                            doPonder();
                        }
                        break;
                    case PLAY_OTHER:
                        forceMode = false;
                        doPonder();
                        break;
                    case PONDER:
                        doPonder();
                        break;
                    case CMD:
                        command.executableCommand.execute();
                        break;
                    case SYNC:
                        //do nothing, just sync
                        break;
                    case MOVE:
                        newlyInitializedGame = false;
                        //deal with ponder
                        int userMove = MV.toBasicMove(command.parameter);
                        //engines are stopped at this point
                        fwConfig.smpController.doSingleMove(StaticMoveGenerator.findLegalMove(fwConfig.smpController.getBoard(), userMove));
                        checkGameStatus();
                        if (!forceMode) {
                            doGo(command.millis);
                            if (ponder && !forceMode){
                                doPonder();
                            }
                        }
                        break;
                    case PING:
                        OutputPrinter.printOutput("pong " + command.parameter);
                        break;
                    default:
                        LOGGER.warn("unimplemented command {}", command.command);
                }
            } catch (InterruptedException ie) {
                LOGGER.warn("engine loop was interrupted", ie);
                throw new RuntimeException("engine loop was interrupted", ie);
            }
        }
    }

    /*
    BEGIN: Commands that we need to react to immediately because the user expects a direct result
     */

    //enter forceMode (force)
    public void forceMode() {
        if (!forceMode) {
            forceMode = true;
            stopEngineAndSync(); // to be sure - also syncs queue - forceMode can be turned off again
            forceMode = true;
        }
    }

    //stop the game (result)
    public void stopGame() {
        if (!forceMode) {
            forceMode = true;
            stopEngineAndSync();
            forceMode = true;
        }
    }

    //move now (?)
    public void moveNow() {
        if (!forceMode) {
            stopEngineAndSync();
        }
    }

    //go
    public void startThinking() {
        if (pondering!=0){
            stopPondering();
        }
        commandQueue.add(new QueuedCommand(Command.GO));
    }

    //if pondering, stop pondering (easy)
    //enable pondering (hard)
    //ponder takes effect immediately, but the engine will only start pondering after a usermove.
    public void setPonder(final boolean ponder) {
        this.ponder = ponder;
        if (pondering!=0){
            stopPondering();
        }
    }

    //undo moves (undo, remove)
    public void undoLastMove(int times) {
        stopEngineAndSync(); //stop any thinking or pondering + sync
        for (int i = 0; i < times; i++) {
            fwConfig.smpController.undoSingleMove();
        }
        if (ponder && !forceMode){
            commandQueue.add(new QueuedCommand(Command.PONDER));
        }
    }

    //new game (new)
    public void setupNewGame() {
        stopEngineAndSync();
        clearEngineState();
        setupStartPosition();
        forceMode = false;
    }

    //new position (setboard)
    public boolean setPosition(final String position) {
        try {
            fwConfig.dummyBoard.setupBoard(position);
            stopEngineAndSync();
            newlyInitializedGame = false;
            fwConfig.smpController.setupBoard(position);
            return true;
        } catch (IllegalFENException e) {
            return false;
        }
    }

    //make sure the engine is initialized (all pending commands, should just be INIT, have completed)
    public void verifyInitialization() {
        try {
            commandQueue.transfer(new QueuedCommand(Command.SYNC));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    //time
    public void setOwnTime(final int ownTime) {
        gameTimer.setOwnTime(ownTime);
    }

    //otim
    public void setOtherTime(final int otherTime) {
        gameTimer.setOtherTime(otherTime);
    }

    //post, nopost
    public void setPost(final boolean post){
        fwConfig.smpController.setPost(post);
    }

    //move .... (stop pondering if pondering)
    public void userMove(String userMove) {
        //todo: after refactor - verify usermove
        int move = MV.toBasicMove(userMove);
        if (forceMode){
            LOGGER.debug("forcing usermove {}", userMove);
            fwConfig.smpController.doSingleMove(StaticMoveGenerator.findLegalMove(fwConfig.smpController.getBoard(), move));
            checkGameStatus();
        } else {
            if (!ponder){
                commandQueue.add(new QueuedCommand(Command.MOVE, userMove));
            } else if (move == pondering){
                doUnderLock(() -> {
                    if (move == pondering) {
                        LOGGER.debug("ponder-hit");
                        pondering = 0;
                        endPonderTime = System.currentTimeMillis();
                        fwConfig.timedSearchStarter.disallowPonder(true);
                    } else {
                        //pondering must have stopped recently
                        commandQueue.add(new QueuedCommand(Command.MOVE, userMove));
                    }
                });
            } else {
                doUnderLock(() -> {
                    if (pondering > 0) {
                        LOGGER.debug("ponder-miss");
                    }
                    fwConfig.smpController.forceStop();
                    fwConfig.timedSearchStarter.disallowPonder(false);
                });
                commandQueue.add(new QueuedCommand(Command.MOVE, userMove));
            }
        }
    }

    /*
    END: Commands that we need to react to immediately because the user expects a direct result
     */


    /*
    BEGIN: Commands that can be queued
     */

    //Initialize the engine, needs to be queued so that other commands will not be executed until this is completed
    public void initializeEngine() {
        commandQueue.add(new QueuedCommand(Command.INIT));
    }

    //Let the GUI know we've completed all pending commands
    public void ping(int pingArgument) {
        commandQueue.add(new QueuedCommand(Command.PING, Integer.toString(pingArgument)));
    }

    //playother
    public void playOtherSide() {
        commandQueue.add(new QueuedCommand(Command.PLAY_OTHER));
    }

    //sd
    public void setMaxSearchDepth(final int maxSearchDepth) {
        commandQueue.add(new QueuedCommand(Command.CMD, "sd: " + maxSearchDepth, () -> fwConfig.smpController.setMaxDepth(maxSearchDepth)));
    }

    //st
    public void setTimeSingleMove(final double time) {
        commandQueue.add(new QueuedCommand(Command.CMD, "st: " + time, () -> {gameTimer.setTimeSingleMove(time);fwConfig.timedSearchStarter.setUseStrictTime(true);}));
    }

    //level
    public void parseLevel(final String fullMovesPerSession, final String time, final String inc) {
        commandQueue.add(new QueuedCommand(Command.CMD, String.format("level: %s %s %s", fullMovesPerSession, time, inc), () -> {doParseLevel(fullMovesPerSession, time, inc);fwConfig.timedSearchStarter.setUseStrictTime(false);}));
    }

    //cores
    public void setCores(int cores) {
        commandQueue.add(new QueuedCommand(Command.CMD, "cores: " + cores, () -> fwConfig.smpController.setCores(cores)));
    }

    /*
    END: Commands that can be queued
     */

    private void doGo(long millis) {
        long startTime = System.currentTimeMillis();
        int startOwnTime = gameTimer.getOwnTime();
        int move = doThink(millis);
        if (move == 0) {
            forceMode = true;
        }
        if (!forceMode) {
            fwConfig.smpController.doSingleMove(move);
            MV.outputMove(move);
            checkGameStatus();
        }
        int thinkingTime = (int)(System.currentTimeMillis()-startTime);
        gameTimer.setTentativeOwnTimeForNextMove(startOwnTime-thinkingTime, fwConfig.smpController.getBoard().getFullMoves());
    }

    private void doParseLevel(String fullMovesPerSessionString, String timeString, String incString){
        String[] timeSubStrings = timeString.split(":");
        double timeSeconds;
        int fullMovesPerSession;
        double inc;
        try {
            inc = Double.parseDouble(incString);
            fullMovesPerSession = Integer.parseInt(fullMovesPerSessionString);
            timeSeconds = 60d * Integer.parseInt(timeSubStrings[0]);
            if (timeSubStrings.length == 2) {
                timeSeconds += Double.parseDouble(timeSubStrings[1]);
            }
        } catch (NumberFormatException nfe) {
            LOGGER.warn("couldn't parse {} {} {} as 'moves [mm:]ss increment'", fullMovesPerSessionString, timeString, incString);
            timeSeconds = 300d;
            fullMovesPerSession = 40;
            inc = 0d;
        }
        gameTimer.parseLevel(fullMovesPerSession, timeSeconds, inc);
    }

    private void doPonder() {
        int ponderMove = findBookMove();
        if (ponderMove == 0){
            ponderMove = findTableBaseMove();
        }
        if (ponderMove == 0) {
            ponderMove = fwConfig.smpController.getBestMoveFromTT();
        }
        if (ponderMove <= 0){
            //todo: shallow search to get pondermove?
            LOGGER.debug("unable to ponder, no move from TT");
        }
        if (ponderMove > 0){
            LOGGER.debug("pondering: " + MV.toString(ponderMove));
            OutputPrinter.printOutput("Hint: " + MV.toString(ponderMove));
            fwConfig.smpController.doSingleMove(ponderMove);
            if (findBookMove()!=0 || findTableBaseMove()!=0){
                //ponder, but just pick the book / tablebase move when pondering stops.
                LOGGER.debug("got reply to pondermove from book / tablebases");
                pondering = -1; // this guarantees the user move will stop pondering.
            } else {
                pondering = MV.toBasicMove(MV.toString(ponderMove));
            }
            //calculate time to move based on tentativeOwnTime (set after thinking) or ownTime (set after usermove)
            long timeToMove = gameTimer.calculateTime((fwConfig.smpController.getBoard().getFullMoves()), 0L);
            int move = fwConfig.timedSearchStarter.getBestMove(true,  timeToMove, new HashSet<>());
            int startOwnTime = gameTimer.getOwnTime(); //set by time command
            doUnderLock(() -> {
                LOGGER.debug("move after ponder: {} (pondering = {})", MV.toString(move), pondering);
                if (move>0 && pondering==0){
                    int thinkingTime = (int)(System.currentTimeMillis()-endPonderTime);
                    fwConfig.smpController.doSingleMove(move);
                    MV.outputMove(move);
                    checkGameStatus();
                    gameTimer.setTentativeOwnTimeForNextMove(startOwnTime-thinkingTime, fwConfig.smpController.getBoard().getFullMoves());
                    if (ponder && !forceMode){
                        commandQueue.add(new QueuedCommand(Command.PONDER));
                    }
                } else {
                    fwConfig.smpController.undoSingleMove();
                }
                pondering = 0;
            });
        }
    }

    private synchronized void doUnderLock(ExecutableCommand executableCommand) {
        executableCommand.execute();
    }

    private class GameStatus {

        private boolean gameOver;
        private String statusLine;

        GameStatus(boolean gameOver, String statusLine) {
            this.gameOver = gameOver;
            this.statusLine = statusLine;
        }

        boolean isGameOver() {
            return gameOver;
        }

        String getStatusLine() {
            return statusLine;
        }

    }

    private void checkGameStatus() {
        GameStatus gameStatus = determineGameStatus();
        if (gameStatus.isGameOver()){
            forceMode = true;
            LOGGER.debug("output: {}", gameStatus.getStatusLine());
            OutputPrinter.printOutput(gameStatus.getStatusLine());
            forceMode = true;
        }
    }

    private GameStatus determineGameStatus(){
        Board board = fwConfig.smpController.getBoard();
        if (board.getQuiet50() > 99) {
            return new GameStatus(true, "1/2-1/2 {Draw by 50 move rule}");
        }
        if (board.checkForRepetitions()) {
            return new GameStatus(true, "1/2-1/2 {Draw by 3fold Repetition}");
        }
        int sideToMove = board.getSideToMove();
        if (StaticMoveGenerator.hasLegalMoves(board)){
            return new GameStatus(false, "game in progress");
        }
        //no legal moves
        if (StaticMoveGenerator.isKingAttacked(board, sideToMove ^ 1)) {
            if (sideToMove == Constants.WHITE) {
                return new GameStatus(true, "0-1 {Black mates}");
            } else {
                return new GameStatus(true, "1-0 {White mates}");
            }
        } else {
            return new GameStatus(true, "1/2-1/2 {Stalemate}");
        }
    }


    private int doThink(long commandTime) {
        int move = findBookMove();
        if (move == 0) {
            move = findTableBaseMove();
        }
        if (move == 0) {
            move = fwConfig.timedSearchStarter.getBestMove(false, gameTimer.calculateTime(fwConfig.smpController.getBoard().getFullMoves(), System.currentTimeMillis() - commandTime), new HashSet<>());
        }
        return move;
    }

    private int findTableBaseMove() {
        Board board = fwConfig.smpController.getBoard();
        if (Constants.USE_TB && tbLoaded && board.isInTableBaseRange()){
            int result = Syzygy.probeDTZ(board);
            if (result==-1) {
                LOGGER.warn("didn't find a move in tablebases");
                return 0;
            }
            //TODO: instead of blindly going for the TB move, first do a small search without TB to see if there is a quick mate.
            int tbMove = Syzygy.toMove(result);
            int score = Syzygy.toXBoardScore(result);
            LOGGER.debug("tablebases returned {} with score {}", MV.toString(tbMove), score);
            if (fwConfig.smpController.getShowThinking()){
                ThoughtLine thoughtLine = new ThoughtLine(score, tbMove);
                OutputPrinter.printObjectOutput(thoughtLine);
            }
            return StaticMoveGenerator.findLegalMove(board, tbMove);
        } else {
            return 0;
        }
    }

    private int findBookMove() {
        int move = 0;
        if (fwConfig.getBook()!=null) {
            int bookMove = book.probeBook(fwConfig.smpController.getBoard().getHashKey());
            if (bookMove != 0) {
                move = StaticMoveGenerator.findLegalMove(fwConfig.smpController.getBoard(), bookMove);
            }
        }
        return move;
    }

    private void stopEngineAndSync() {
        commandQueue.clear();
        fwConfig.smpController.forceStop();
        try {
            //wait till all queued commands are executed (normal state) stopping the engine as needed
            //this takes care of a stopEngineAndSync command while the engine is just being started
            while(!commandQueue.tryTransfer(new QueuedCommand(Command.SYNC), 100, TimeUnit.MILLISECONDS)){
                fwConfig.smpController.forceStop();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopPondering() {
        fwConfig.timedSearchStarter.stopPondering();
        fwConfig.smpController.forceStop();
    }

    private void clearEngineState() {
        fwConfig.smpController.setMaxDepth(fwConfig.getMaxDepth());
        fwConfig.smpController.clearCaches();
    }

    private void setupStartPosition() {
        try {
            fwConfig.smpController.setupBoard(Constants.STARTPOS);
        } catch (IllegalFENException e) {
            throw new IllegalStateException("cannot load start position", e);
        }
    }

    private void ensureEngineStopped() {
        //Should always be the case - engine activity is coordinated via this class
    }


    private void loadOpeningBook() {
        book = new SimpleBookImpl();
        String bookName = fwConfig.getBook();
        if (bookName != null) {
            book.loadBook(bookName);
        }
    }
}
