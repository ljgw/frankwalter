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

import java.util.EnumMap;
import java.util.Map;

/**
 * enumeration of commandline arguments with their detaults
 */
public enum CommandLineArgument {
    DEBUG("-debug"), TT_SIZE("-tt"), TB_LOCATION("-tb"), BOOK("-book"), NO_BOOK("-nobook"), EPD("-epd");

    private String argument;

    CommandLineArgument(String argument){
        this.argument = argument;
    }

    public String getArgument() {
        return argument;
    }

    public static Map<CommandLineArgument, Object> getBaseProperties(){
        Map<CommandLineArgument, Object> defaults = new EnumMap<>(CommandLineArgument.class);
        defaults.put(DEBUG, Boolean.FALSE);
        defaults.put(TT_SIZE, "256mb");
        defaults.put(TB_LOCATION, null);
        defaults.put(BOOK, "frankwalter.openings");
        defaults.put(NO_BOOK, Boolean.FALSE);
        return defaults;
    }

    public static CommandLineArgument parse(String argumentString) {
        for (CommandLineArgument commandLineArgument : CommandLineArgument.values()){
            if (commandLineArgument.getArgument().equalsIgnoreCase(argumentString)){
                return commandLineArgument;
            }
        }
        return null;
    }
}
