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

import com.winkelhagen.chess.frankwalter.config.FWConfig;
import com.winkelhagen.chess.frankwalter.FrankWalter;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;

public abstract class EpdProcessor {
    private static final String[] EPD_DEFAULT_ARGS = {"-debug", "-tt", "16mb"};
    private final String id;
    protected FWConfig fwConfig;

    public EpdProcessor(String id) {
        this.id = id;
        fwConfig = FrankWalter.getFWConfig();
        if (fwConfig == null){
            //This would occur when EpdProcessor is used outside of the FrankWalter process (for example in a unit test)
            fwConfig = new FWConfig(EPD_DEFAULT_ARGS);
        }
        fwConfig.preloadStaticClasses();
        fwConfig.setTranspositionTable();

    }

    public boolean toBeProcessed(ExtendPositionDescription epd) {
        return id==null || "".equals(id) || id.equals(epd.getOpCodeValue(EpdOpCode.ID));
    }

    public abstract String process(ExtendPositionDescription epd);

    public boolean setup(ExtendPositionDescription epd){
        try {
            fwConfig.engine.clearCaches();
            fwConfig.board.setupBoard(epd.getFen());
        } catch (IllegalFENException e) {
            return false;
        }
        return true;
    }

    public abstract String getResult();

    public abstract boolean filter(String lineResult);

    public void printSingleStatistics(){
        if (id!=null && !"".equals(id)) {
            printStatistics();
        }
    }

    protected void printStatistics(){
        //default
    }
}
