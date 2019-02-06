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
package com.winkelhagen.chess.frankwalter.engine.tb;

import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.board.BoardView;
import com.winkelhagen.chess.frankwalter.util.MV;
import com.winkelhagen.chess.syzygy.SyzygyBridge;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by laurens on 16-9-18 for frankwalter.
 */
public class SyzygyTest {

    private static String STARTPOS = "8/8/8/3K4/5k2/7b/4Bp2/8 b - - 1 71";

    @Test public void testLibrary(){
        assertTrue(SyzygyBridge.isLibLoaded());
    }

    @Test
    public void testLoadingTBs() throws IllegalFENException {
        assertTrue(0 >= SyzygyBridge.load("/should/not/exist"));
        assertFalse(Syzygy.isAvailable(5));
    }

    @Test
    public void testFunctions() throws IllegalFENException {
        assertEquals(0, Syzygy.getWDLScore(2, 30));
        assertEquals("h1g2", MV.toString(Syzygy.toMove(4201696)));
        assertEquals(-27998, Syzygy.toXBoardScore(4201696));
    }

}