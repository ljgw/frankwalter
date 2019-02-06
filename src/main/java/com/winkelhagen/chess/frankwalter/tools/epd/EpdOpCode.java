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

/**
 * Created by laurens on 29-12-18 for frankwalter.
 */
public enum EpdOpCode {
    BM, ID, PERFT1, PERFT2, PERFT3, PERFT4, PERFT5, PERFT6, PERFT7, PERFT8, PERFT9;

    public static final EpdOpCode[] PERFT_OP_CODES = {PERFT1, PERFT2, PERFT3, PERFT4, PERFT5, PERFT6, PERFT7, PERFT8, PERFT9};

}
