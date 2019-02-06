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
package com.winkelhagen.chess.frankwalter.config;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.engine.Engine;
import com.winkelhagen.chess.frankwalter.engine.ScoutEngineImpl;
import com.winkelhagen.chess.frankwalter.engine.TimedSearchStarter;
import com.winkelhagen.chess.frankwalter.engine.tt.TranspositionTableArrayImpl;
import com.winkelhagen.chess.frankwalter.util.BB;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.winkelhagen.chess.frankwalter.config.CommandLineArgument.*;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central class responsible for holding the FrankWalter config. Currently config can be supplied only via commandline parameters.
 * Some options can be then overwritten via xboard commands.
 */
public class FWConfig {

	private static final int DEFAULT_TT_MAGNITUDE = 24;
	private static final String TT_SIZE_PATTERN = "^(?<size>\\d++)(?<quantifier>\\w+)?$";

	private static final Logger logger = LogManager.getLogger();
	public final Board board = new Board();
	public final Engine engine = new ScoutEngineImpl();
	public final Board dummyBoard = new Board();

	private Map<CommandLineArgument, Object> properties = CommandLineArgument.getBaseProperties();
	public final TimedSearchStarter timedSearchStarter = new TimedSearchStarter(engine);

    public FWConfig(String[] args){
		parseArguments(args);

		engine.setBoard(board);

		testTableBaseLibrary();
	}

	private void testTableBaseLibrary() {
		SyzygyBridge.isLibLoaded();
	}

	public void logProperties(){
		for (Map.Entry<CommandLineArgument, Object> entry : properties.entrySet()){
			logger.debug("{} : {}", entry.getKey(), entry.getValue());
		}
	}

	private void parseArguments(String[] args) {
		if (args==null){
			return;
		}
		CommandLineArgument argumentToSet = null;
		for (String arg : args){
			if (arg.startsWith("-") && argumentToSet!=null){
				logger.warn("Unexpected commandline parameter, expected an argument for the '{}' option.", argumentToSet.getArgument());
				argumentToSet = null;
			}
			if (argumentToSet != null){
				setArgument(argumentToSet, arg);
				argumentToSet = null;
			} else {
				argumentToSet = parseCommandLineParameter(arg);
			}
		}
	}

	private CommandLineArgument parseCommandLineParameter(String argumentString) {
		CommandLineArgument commandLineArgument = CommandLineArgument.parse(argumentString);
		if (commandLineArgument==null){
			logger.warn("Unknown commandline parameter '{}'.", argumentString);
		} else {
			switch (commandLineArgument) {
				case DEBUG:
					properties.put(DEBUG, Boolean.TRUE);
					break;
				case NO_BOOK:
					properties.put(NO_BOOK, Boolean.TRUE);
					break;
				case BOOK:
				case EPD:
				case TB_LOCATION:
				case TT_SIZE:
					return commandLineArgument;
				default:
					logger.warn("Unimplemented commandline parameter '{}'.", argumentString);
			}
		}
		return null;
	}

	private void setArgument(CommandLineArgument argumentToSet, String arg) {
		switch (argumentToSet){
			case TT_SIZE:
				properties.put(TT_SIZE, arg);
				break;
			case TB_LOCATION:
				File tbLocation = new File(arg);
				if (tbLocation.exists() && tbLocation.isDirectory()) {
					properties.put(TB_LOCATION, tbLocation.getAbsolutePath());
				}
				break;
			case BOOK:
				properties.put(BOOK, arg);
				break;
			case EPD:
				properties.put(EPD, arg);
				break;
			default:
				//should not get here.
		}
	}

	public int getTTSize(int max) {
		int size = DEFAULT_TT_MAGNITUDE; // default
		String sizeArgument = ((String) properties.get(TT_SIZE)).trim().toLowerCase();
		Matcher matcher = Pattern.compile(TT_SIZE_PATTERN).matcher(sizeArgument);
        if (matcher.find()){
			int quantifiedSize = Integer.parseInt(matcher.group("size"));
            String quantifier = matcher.group("quantifier");
            if (quantifier == null){
                size = quantifiedSize;
            } else {
                switch (quantifier) {
                    case "gb":
                        size = BB.msb((long) quantifiedSize * 1024 * 1024 * 1024 / 8 / 2);
                        break;
                    case "mb":
                        size = BB.msb((long) quantifiedSize * 1024 * 1024 / 8 / 2);
                        break;
                    case "kb":
                        size = BB.msb((long) quantifiedSize * 1024 / 8 / 2);
                        break;
                    default:
                        logger.warn("unknown TT size quantifier: {}. using default size (256mb)", quantifier);
                        size = DEFAULT_TT_MAGNITUDE;
                }
			}
		} else {
            logger.warn("unparsable TT size argument: {}. using default size (256mb)", sizeArgument);
        }
		return Math.min(max, size);
	}

	public int getMaxDepth() {
		return 100;
	}

	public boolean isDebug() {
		return Boolean.class.cast(properties.get(DEBUG));
	}

	public String getTBLocation() {
		return String.class.cast(properties.get(TB_LOCATION));
	}

	public String getBook(){
		if (Boolean.TRUE.equals(properties.get(NO_BOOK))){
			return null;
		} else {
			return String.class.cast(properties.get(BOOK));
		}
	}

	public String getEpd(){
		return String.class.cast(properties.get(EPD));
	}

    private int getMaxTTSize() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        int max;
        if (maxMemory<=66650112){
            max = 18;
        } else if (maxMemory<=276824064){
            max = 21;
        } else if (maxMemory<=399572992){
            max = 24;
        } else if (maxMemory<=3004094720L){
            max = 25;
        } else {
            max = 26;
        }
        return max;
    }

	public void preloadStaticClasses(){
		try {
			//to prevent timeouts on the first game
			Class.forName("com.winkelhagen.chess.frankwalter.engine.evaluator.Evaluator");
			Class.forName("com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("class not found", e);
		}
	}

	public void setTranspositionTable() {
		int magnitude = getTTSize(getMaxTTSize());
		logger.info("initializing TranspositionTables with magnitude {} ({} MB)", magnitude, (1<<(magnitude-16)));
		engine.setTranspositionTable(new TranspositionTableArrayImpl(magnitude));
	}

}
