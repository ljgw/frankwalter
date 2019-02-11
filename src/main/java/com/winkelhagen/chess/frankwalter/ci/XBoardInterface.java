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
package com.winkelhagen.chess.frankwalter.ci;

import com.winkelhagen.chess.frankwalter.GameController;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class XBoardInterface extends AbstractInterface {
    private static final String COMMAND_NOT_SUPPORTED = "command {} not supported.";
    private static final String UNEXPECTED_NUMBER_OF_ARGUMENTS = "command {} didn't have the expected number of arguments";
    private static Logger logger = LogManager.getLogger();
    private static final int STATE_OFF = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_XBOARD = 2;
    private int state = STATE_OFF;

    public XBoardInterface(GameController ui) {
        super(ui);
    }


    @Override
    protected void userInputLoop() {
        logger.info("Started Userinterface");
        game.startEngineLoop();
        state = STATE_STARTING;
        while (state != STATE_OFF) {
            String userInput = null;
            try {
                userInput = bufferedReader.readLine();
            } catch (IOException ioe) {
                logger.error("IO error reading input", ioe);
                state = STATE_OFF;
                break;
            }
            try {
                processUserInput(userInput);
            } catch (RuntimeException rte) {
                logger.error("Error (malformatted command): {}", userInput);
                logger.debug(rte);
            }
        }

        game.stopEngineLoop();
        logger.info("Stopped Userinterface");
    }

    private void processUserInput(String userInput) {
        logger.debug("UserInput = '{}'", userInput);

        if (state == STATE_STARTING) {
            switch (userInput){
                case "quit":
                    state = STATE_OFF;
                    break;
                case "xboard":
                    state = STATE_XBOARD;
                    game.initializeEngine();
                    break;
                default:
                    break;
            }
        } else if (state == STATE_XBOARD) {
            processXboardCommand(userInput);
        }
    }

    private void processXboardCommand(String userInput) {
        String[] userInputSubstrings = userInput.split(" ");
        String command = userInputSubstrings[0].toLowerCase();
        switch (command){
            case "quit":
                state = STATE_OFF;
                break;
            case "protover":
                protoverCommand(Integer.parseInt(userInputSubstrings[1]));
                break;
            case "accepted":
                break;
            case "rejected":
                logger.debug("feature was rejected {}", userInput);
                break;
            case "new":
                game.setupNewGame();
                break;
            case "egtpath":
                loadEndgameTablebases(userInputSubstrings, command);
                break;
            case "variant":
            case "random":
            case "white":
            case "black":
            case "draw":
            case "edit":
            case "hint":
            case "bk":
            case "post":
            case "nopost":
            case "analyze":
            case "name":
            case "rating":
            case "ics":
            case "computer":
                logger.info(COMMAND_NOT_SUPPORTED, command);
                break;
            case "force":
                game.forceMode();
                break;
            case "go":
                game.startThinking();
                break;
            case "playother":
                game.playOtherSide();
                break;
            case "level":
                if (userInputSubstrings.length>2) {
                    game.parseLevel(userInputSubstrings[1], userInputSubstrings[2].replace(',','.'),
                            userInputSubstrings[3].replace(',','.'));
                } else {
                    logger.info(UNEXPECTED_NUMBER_OF_ARGUMENTS, command);
                }
                break;
            case "st":
                game.setTimeSingleMove(Double.parseDouble(userInputSubstrings[1]));
                break;
            case "sd":
                game.setMaxSearchDepth(Integer.parseInt(userInputSubstrings[1]));
                break;
            case "time":
                game.setOwnTime(Integer.parseInt(userInputSubstrings[1]));
                break;
            case "otim":
                game.setOtherTime(Integer.parseInt(userInputSubstrings[1]));
                break;
            case "?":
                game.moveNow();
                break;
            case "result":
                game.stopGame();
                break;
            case "setboard":
                setBoardCommand(userInputSubstrings);
                break;
            case "ping":
                int pingArgument = Integer.parseInt(userInputSubstrings[1]);
                logger.debug("ping {}", pingArgument);
                pingCommand(pingArgument);
                break;
            case "undo":
                game.undoLastMove(1);
                break;
            case "remove":
                game.undoLastMove(2);
                break;
            case "hard":
                game.setPonder(true);
                break;
            case "easy":
                game.setPonder(false);
                break;
            case "usermove":
                game.userMove(userInputSubstrings[1]);
                break;
            default:
                OutputPrinter.printOutput("Error (unknown command): " + userInput);
                logger.error("Error (unknown command): {}", userInput);
                break;
        }
    }

    private void loadEndgameTablebases(String[] userInputSubstrings, String command) {
        if (userInputSubstrings[1].equalsIgnoreCase("syzygy")) {
            if (userInputSubstrings.length > 2) {
                if (SyzygyBridge.isLibLoaded()) {
                    int size = SyzygyBridge.load(userInputSubstrings[2]);
                    logger.info("loaded TB {} from location {} (size: {})", userInputSubstrings[1], userInputSubstrings[2], size);
                } else {
                    logger.info("ignoring TB location because JSyzygy library failed to load");
                }
            } else {
                logger.info(UNEXPECTED_NUMBER_OF_ARGUMENTS, command);
            }
        }
    }

    /**
     * respond to the ping command
     *
     * @param pingArgument
     *            the number to ping back
     */
    private void pingCommand(int pingArgument) {
        logger.debug("pong {} queue", pingArgument);
        game.ping(pingArgument);
    }

    /**
     * Respond to the setBoard command
     *
     * @param userInputSubstrings
     *            the user input, to be converted into FEN
     */
    private void setBoardCommand(String[] userInputSubstrings) {
        StringBuilder fen = new StringBuilder("");
        for (int i = 1; i < userInputSubstrings.length; i++) {
            fen.append(userInputSubstrings[i]);
            fen.append(" ");
        }
        if (!game.setPosition(fen.toString())) {
            OutputPrinter.printOutput("tellusererror Illegal position");
        }
    }

    /**
     * Respond to the protover command
     *
     * @param version
     *            the protocol version of the client
     */
    private void protoverCommand(int version) {
        if (version > 1) {
            OutputPrinter.printOutput("feature done=0");
            if (SyzygyBridge.isLibLoaded()) {
                OutputPrinter.printOutput("feature egt=\"syzygy\"");
            }
            OutputPrinter.printOutput("feature setboard=1");
            OutputPrinter.printOutput("feature playother=1");
            OutputPrinter.printOutput("feature usermove=1");
            OutputPrinter.printOutput("feature debug=1");
            OutputPrinter.printOutput("feature colors=0");
            OutputPrinter.printOutput("feature analyze=0");
            OutputPrinter.printOutput("feature sigint=0");
            OutputPrinter.printOutput("feature variants=\"normal\"");
            OutputPrinter.printOutput("feature myname=\"" + Constants.getEngineName() + "\"");

            game.verifyInitialization();
            OutputPrinter.printOutput("feature done=1");
        }
    }

}
