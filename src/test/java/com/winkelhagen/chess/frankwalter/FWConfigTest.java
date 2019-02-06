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
package com.winkelhagen.chess.frankwalter;

import com.winkelhagen.chess.frankwalter.config.FWConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by laurens on 14-11-18 for frankwalter.
 */
public class FWConfigTest {
    @Test
    public void testTTSizemb(){
        String[] ARGS = {"-debug", "-tt", "16mb"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size 16mb should be 20 magnitude", 20, size);
    }

    @Test
    public void testTTSizeCaseSensitivity(){
        String[] ARGS = {"-debug", "-tt", "16MB"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size 16MB should be 20 magnitude", 20, size);
    }

    @Test
    public void testTTSizeMagnitude(){
        String[] ARGS = {"-debug", "-tt", "20"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size 20 should be 20 magnitude", 20, size);
    }

    @Test
    public void testTTSizeDefaultQuantifier(){
        String[] ARGS = {"-debug", "-tt", "16PB"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size 'unkown quantifier' should be 24 magnitude", 24, size);
    }

    @Test
    public void testTTSizeDefaultUnparsable(){
        String[] ARGS = {"-debug", "-tt", "s 16PB"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size 'unparsable' should be 24 magnitude", 24, size);
    }

    @Test
    public void testTTSizeDefault(){
        String[] ARGS = {"-debug"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size unknown should be 24 magnitude", 24, size);
    }

    @Test
    public void testTTSizeMax(){
        String[] ARGS = {"-debug", "-tt", "16GB"};
        int size = new FWConfig(ARGS).getTTSize(26);
        assertEquals("Size too large should scale back to 26 magnitude", 26, size);
    }


}