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

import com.winkelhagen.chess.frankwalter.engine.moves.StaticMoveGenerator;

import java.util.ArrayList;
import java.util.List;

public class PerftProcessor extends EpdProcessor {

    private int[][] movesTable = new int[10][256];

    private int positions = 0;
    private int solves = 0;

    public PerftProcessor(String id) {
        super(id);
    }

    @Override
    public String process(ExtendPositionDescription epd) {
        positions++;
        solves++;
        List<String> results = new ArrayList<>();
        for (EpdOpCode perftOpCode : EpdOpCode.PERFT_OP_CODES){
            String value = epd.getOpCodeValue(perftOpCode);
            if (value!=null){
                if (verifyPerft(Integer.parseInt(value), Integer.parseInt(perftOpCode.name().substring(5)))){
                    results.add(perftOpCode.name());
                } else {
                    results.add(String.format("Failed: %s", perftOpCode.name()));
                    solves--;
                    break;
                }
            }
        }
        return String.join(" ", results);
    }

    private boolean verifyPerft(int nodes, int depth) {
        return (nodes == perft(depth));
    }

    private long perft(int depth){
        long nodeCount = 0;
        int moves[] = movesTable[depth];
        int movesNr = StaticMoveGenerator.generateLegalMoves(fwConfig.smpController.getBoard(), moves);

        if (depth==1){
            return movesNr;
        }
        for (int i=0; i<movesNr; i++){
            fwConfig.smpController.doSingleMove(moves[i]);
            nodeCount += perft(depth-1);
            fwConfig.smpController.undoSingleMove();
        }
        return nodeCount;
    }

    @Override
    public String getResult() {
        return String.format("Result: %d/%d", solves, positions);
    }

    @Override
    public boolean filter(String lineResult) {
        return lineResult.contains("Failed");
    }
}
