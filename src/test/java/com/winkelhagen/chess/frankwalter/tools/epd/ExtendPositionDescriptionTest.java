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

import org.junit.Test;

import static org.junit.Assert.*;

public class ExtendPositionDescriptionTest {

    @Test
    public void testFenOnly(){
        String startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -";
        ExtendPositionDescription epd = new ExtendPositionDescription(startPosition);
        assertEquals("full position from fen should be parsed", startPosition, epd.getFen());
    }

    @Test
    public void testNormalBM(){
        String epdString = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - bm Qd1+; id \"BK.01\";";
        ExtendPositionDescription epd = new ExtendPositionDescription(epdString);
        assertEquals("full position from fen should be parsed", "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -", epd.getFen());
        assertEquals("bm is Qd1+", "Qd1+", epd.getOpCodeValue(EpdOpCode.BM));
        assertEquals("id is BK.01", "BK.01", epd.getOpCodeValue(EpdOpCode.ID));
    }

    @Test
    public void testOneOpCode(){
        String epdString = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - bm Qd1+;";
        ExtendPositionDescription epd = new ExtendPositionDescription(epdString);
        assertEquals("full position from fen should be parsed", "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -", epd.getFen());
        assertEquals("bm is Qd1+", "Qd1+", epd.getOpCodeValue(EpdOpCode.BM));
    }

    @Test
    public void testLenientMissingLastSemiColon(){
        String epdString = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - bm Qd1+";
        ExtendPositionDescription epd = new ExtendPositionDescription(epdString);
        assertEquals("full position from fen should be parsed", "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -", epd.getFen());
        assertEquals("bm is Qd1+", "Qd1+", epd.getOpCodeValue(EpdOpCode.BM));
    }

    @Test
    public void testLenientExtraSemiColon(){
        String epdString = "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - - ;";
        ExtendPositionDescription epd = new ExtendPositionDescription(epdString);
        assertEquals("full position from fen should be parsed", "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 b - -", epd.getFen());
    }

}