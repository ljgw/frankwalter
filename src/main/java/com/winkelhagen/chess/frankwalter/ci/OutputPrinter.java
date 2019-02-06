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

import java.io.PrintStream;

/**
 * this separate class for output can be used to redirect all output to a different out stream - might be useful when testing
 */
public class OutputPrinter {

	private OutputPrinter(){}

	private static PrintStream out = System.out; //NOSONAR

	/**
	 * prints the string representation of the Object argument
	 * @param object the object to print (most likely a {@link com.winkelhagen.chess.frankwalter.engine.moves.ThoughtLine})
	 */
	public static void printObjectOutput(Object object) {
        printOutput(String.valueOf(object));
	}
	
	public static void printOutput(String output) {
		out.println(output);
	}

	public static void setOutputPrintStream(PrintStream newOut){
		out = newOut;
	}

}
