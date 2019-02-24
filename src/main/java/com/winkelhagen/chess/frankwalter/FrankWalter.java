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

import com.winkelhagen.chess.frankwalter.ci.XBoardInterface;
import com.winkelhagen.chess.frankwalter.config.FWConfig;
import com.winkelhagen.chess.frankwalter.util.Constants;
import com.winkelhagen.chess.frankwalter.tools.epd.EpdReader;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.util.Map;

/**
 * FrankWalter is a winboard compatible chess program written in Java, this is it's Main Class.
 * Start with java -jar fw.jar, it obeys protocol version 2 of the winboard protocol.
 * 
 * See enclosed read-me or other sources for more information.
 * @author Laurens Winkelhagen
 *
 */
public class FrankWalter {
	private static Logger logger = LogManager.getLogger();
	private static FWConfig fwConfig;

	private FrankWalter(){
	}

	/**
	 * main method of Frank-Walter: use it to start the engine.
	 * @param args commandline arguments
	 */
	public static void main(String[] args )	{
		Thread.currentThread().setName("MAIN");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		fwConfig = new FWConfig(args);
		rerouteLogging(fwConfig);
		logger.info("Started FrankWalter {}", Constants.getVersion());
		fwConfig.logProperties();
		if (fwConfig.getEpd()!=null){
			EpdReader epdReader = EpdReader.create(fwConfig.getEpd());
			if (epdReader!=null) {
				epdReader.process();
				epdReader.printResults();
			}
		} else {
			loadTablebases(fwConfig);
			registerShutdownHook();
			play(fwConfig);
		}
	}

	private static void rerouteLogging(FWConfig fwConfig) {
		if (fwConfig.isDebug()) {
			LoggerContext ctx = ((LoggerContext) LogManager.getContext(false));
			Configuration config = ctx.getConfiguration();

			config.getRootLogger().setLevel(Level.DEBUG);
			config.getRootLogger().addAppender(config.getAppender("async-file"), null, null);
			config.getRootLogger().removeAppender("console");
			ctx.updateLoggers();
		}
	}

	private static void loadTablebases(FWConfig fwConfig) {
		String tbLocation = fwConfig.getTBLocation();
		if (tbLocation!=null && SyzygyBridge.isLibLoaded()) {
			int tbLargest = SyzygyBridge.load(tbLocation);
			logger.info("loaded Syzygy tablebases at {}: {}", tbLocation, tbLargest);
		}
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (Map.Entry<Thread, StackTraceElement[]> entry: Thread.getAllStackTraces().entrySet()){
				logger.debug("{} {}", entry.getKey().getName(), entry.getValue().length!=0?entry.getValue()[0].toString(): "no frame");
			}
		},"shutdownHook"));
	}

	/**
	 * Starts the game engine, as well as a thread for user input
	 */
	private static void play(FWConfig fw){
		XBoardInterface gameInterface = new XBoardInterface(new GameController(fw));
		gameInterface.processUserInput();
	}

	public static FWConfig getFWConfig() {
		return fwConfig;
	}
}
