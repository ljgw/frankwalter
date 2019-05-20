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


public enum WDL {
    WIN("1-0", 1d), DRAW("1/2-1/2", 0.5d), LOSS("0-1", 0d);

    private String result;
    private double value;

    WDL(String result, double value) {
        this.result = result;
        this.value = value;
    }

    public String getResult(){
        return result;
    }

    public double getValue() {
        return value;
    }

}