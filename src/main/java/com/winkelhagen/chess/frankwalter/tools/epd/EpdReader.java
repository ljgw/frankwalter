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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EpdReader {

    private static final Logger logger = LogManager.getLogger();

    private String fileName;
    private EpdProcessor epdProcessor;
    private String result;
    private List<String> warnings = new ArrayList<>();
    private List<String> lineResults = new ArrayList<>();

    public static EpdReader create(String creationString){
        String[] splits = creationString.split(":");
        if (splits.length!=3 && splits.length!=4){
            logger.warn("EpdReader creationString {} is not of format <type>:<millis>:<file>[:id]", creationString);
            return null;
        }
        switch (splits[0].toLowerCase()){
            case "bm":
                return new EpdReader(splits[2], new BestMoveProcessor(Integer.parseInt(splits[1]), splits.length==3?null:splits[3]));
            default:
                logger.warn("unsupported EPD type '{}'", splits[0]);
                return null;
        }
    }

    public EpdReader(String fileName, EpdProcessor epdProcessor){
        this.fileName = fileName;
        this.epdProcessor = epdProcessor;
    }

    public void process(){
        File epdFile = new File(fileName);
        if (!epdFile.exists() || !epdFile.isFile()){
            warnings.add(String.format("suite '%s' is not an epd file.", fileName));
            logger.error("suite '{}' is not an epd file.", fileName);
            return;
        }
        try (Scanner scanner = new Scanner(epdFile)){
            while (scanner.hasNext()){
                //TODO: do this in parallel over usable threads - great test for parallel execution!
                ExtendPositionDescription epd = new ExtendPositionDescription(scanner.nextLine());
                if (epdProcessor.toBeProcessed(epd)){
                    if (epdProcessor.setup(epd)){
                        logger.debug("processing epd '{}'", epd);
                        lineResults.add(epdProcessor.process(epd));
                    } else {
                        warnings.add(String.format("Unable to setup position '%s' for id '%s'", epd.getFen(), epd.getOpCodeValue(EpdOpCode.ID)));
                    }
                }
            }
            result = epdProcessor.getResult();
        } catch (FileNotFoundException e) {
            result = "Error";
            warnings.add(String.format("suite '%s' is not an epd file.", fileName));
            logger.error("suite '{}' is not an epd file.", fileName);
            logger.debug(e);
        }
    }

    public String getResult() {
        return result;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getLineResults() {
        return lineResults;
    }

    public void printResults() {
        warnings.forEach(System.out::println);
        System.out.println(result);
        lineResults.stream().filter(lineResult -> epdProcessor.filter(lineResult)).forEach(System.out::println);
        epdProcessor.printSingleStatistics();
    }
}
