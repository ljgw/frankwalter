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
package com.winkelhagen.chess.frankwalter.tools.epd;

import java.util.EnumMap;
import java.util.Map;

public final class ExtendPositionDescription {
    private String fen;
    private Map<EpdOpCode, String> opCodeMap = new EnumMap<>(EpdOpCode.class);
    private String epdString;

    public ExtendPositionDescription(String line){
        this.epdString = line;
        String[] fenParts = new String[4];
        int end = 0;
        for (int i = 0; i<fenParts.length; i++){
            int start = end;
            end = line.indexOf(' ', start);
            if (end==-1){
                end = line.length();
            }
            fenParts[i] = line.substring(start, end++);
        }
        fen = String.join(" ", fenParts);
        String opCodeStrings = line.substring(--end);
        for (String opCodeString : opCodeStrings.split(";")) {
            opCodeString = opCodeString.trim();
            if ("".equals(opCodeString)){
                continue;
            }
            int split = opCodeString.indexOf(' ');
            String opCode = opCodeString.substring(0, split).toUpperCase();
            String value = opCodeString.substring(split+1);
            if (value.matches("^\".*\"$")){
                value = value.substring(1, value.length()-1);
            }
            opCodeMap.put(EpdOpCode.valueOf(opCode), value);
        }
    }

    public final String getFen(){
        return fen;
    }

    public final String getOpCodeValue(EpdOpCode epdOpCode){
        return opCodeMap.get(epdOpCode);
    }

    @Override
    public final String toString(){
        return epdString;
    }
}
