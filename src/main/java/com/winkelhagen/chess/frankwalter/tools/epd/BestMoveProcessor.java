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

import com.winkelhagen.chess.frankwalter.board.BoardView;
import com.winkelhagen.chess.frankwalter.engine.TimedSearchStarter;
import com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator;
import com.winkelhagen.chess.frankwalter.util.MV;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class allows one to run a bm (best moves) based EPD file from the commandline.
 * As an example the following command can be ran from the target directory:
 * <pre>java -jar frankwalter.jar -epd bm:10000:../wac.epd:WAC.002</pre>
 */
public class BestMoveProcessor extends EpdProcessor {

    private int millisToSolve;

    private int positions = 0;
    private int solves = 0;

    public BestMoveProcessor(int millisToSolve, String id){
        super(id);
        this.millisToSolve = millisToSolve;
        fwConfig.timedSearchStarter.setUseStrictTime(true);
    }

    @Override
    public String process(ExtendPositionDescription epd) {
        positions++;
        List<String> epdBestMoves = new ArrayList<>();
        for (String bestMove : epd.getOpCodeValue(EpdOpCode.BM).trim().split(" ")){
            epdBestMoves.add(MV.toString(StaticMoveGenerator.parseSAN(fwConfig.smpController.getBoard(), bestMove)));
        }
        String engineBestMove = MV.toString(fwConfig.timedSearchStarter.getBestMove(false, millisToSolve, new HashSet<>()));
        String result;
        if (epdBestMoves.contains(engineBestMove)){
            solves++;
            result = "Solved";
        } else {
            result = "Failed to solve";
        }
        return String.format("%s epd '%s' ('%s' - '%s') engine found: '%s'",
                result,
                epd.getOpCodeValue(EpdOpCode.ID),
                epd.getFen(),
                String.join(" ", epdBestMoves),
                engineBestMove
        );
    }

    @Override
    public String getResult(){
        return String.format("Result: %d/%d @ %d milliseconds per position", solves, positions, millisToSolve);
    }

    @Override
    public boolean filter(String lineResult) {
        return lineResult.startsWith("Failed to solve");
    }

    @Override
    public void printStatistics() {
        new BoardView(fwConfig.smpController.getBoard()).echoPosition();
        fwConfig.smpController.printStatistics();
    }
}
