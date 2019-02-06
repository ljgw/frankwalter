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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore
public class EpdReaderTest {

    @Test
    public void testProcessWAC(){
        EpdReader epdReader = new EpdReader("wac.epd", new EpdProcessor(null) {
            @Override
            public String process(ExtendPositionDescription epd) {
                return null;
            }

            @Override
            public String getResult() {
                return "success!";
            }

            @Override
            public boolean filter(String lineResult) {
                return false;
            }
        });
        epdReader.process();
        assertEquals("no warnings expected", 0, epdReader.getWarnings().size());
        assertEquals("300 line results expected", 300, epdReader.getLineResults().size());
        assertEquals("end result is success!", "success!", epdReader.getResult());
    }

    @Test
    public void testSingleRealWAC(){
        EpdReader epdReader = new EpdReader("wac.epd", new BestMoveProcessor(10000, "WAC.002"));
        epdReader.process();
        epdReader.printResults();
        assertEquals("no warnings expected", 0, epdReader.getWarnings().size());
        assertEquals("1 line results expected", 1, epdReader.getLineResults().size());
    }

    @Test
    public void testPerft(){
        EpdReader epdReader = new EpdReader("perftsuite.epd", new PerftProcessor(null));
        epdReader.process();
        epdReader.printResults();
        assertEquals("end result is Result: 126/126", "Result: 126/126", epdReader.getResult());
    }

}