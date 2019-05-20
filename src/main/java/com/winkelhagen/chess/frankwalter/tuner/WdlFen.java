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
package com.winkelhagen.chess.frankwalter.tuner;

import com.winkelhagen.chess.frankwalter.util.IllegalFENException;

import java.util.HashMap;
import java.util.Map;

/**
 * class to describe chesspositions conforming EPD
 * @author Lau
 *
 */
public class WdlFen implements Valuatable {

    private static Map<String, WDL> map;
    private Integer valuationCache;
    private static double k;
    private static boolean useCache = false;

    static {
        map = new HashMap<>();
        for (WDL wdl : WDL.values()){
            map.put(wdl.getResult(), wdl);
        }
    }

    private String fen;
    private WDL wdl;

    public WdlFen(String fen, String result){
        this.fen = fen;
        this.wdl = map.get(result);
        assert (this.getWdl()!=null);
    }

    public static void setUseCache(boolean useCache) {
        WdlFen.useCache = useCache;
    }

    /**
     * @return the fen
     */
    public String getFen() {
        return fen;
    }

    public WDL getWdl() {
        return wdl;
    }


    @Override
    public double valuationError(Environment environment){
        int valuation;
        if (!useCache || valuationCache==null){
            try {
                environment.setupBoard(fen);
//                valuation = environment.getQScore();
                valuation = environment.eval();
            } catch (IllegalFENException e) {
                throw new IllegalStateException("illegal fen: " + fen);
            }
            if (useCache) {
                valuationCache = valuation;
            }
        } else {
            valuation = valuationCache;
        }
        return Math.pow(wdl.getValue() - sigmoid(valuation),2d);
    }

    private double sigmoid(double valuation){
        double power = -k * valuation/400d;
        return 1/(1+Math.pow(10, power));
    }

    public static void setK(double k){
        WdlFen.k = k;
    }

}