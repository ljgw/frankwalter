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

import com.winkelhagen.chess.frankwalter.engine.evaluator.Evaluator;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.BB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TuningEpdReader {

    /*
    #[INFO ] 2019-03-31 22:04:42.682 [main] TuningEpdReader - result [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, -1, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 1, 0]

time in millis: 3293993 K: 1.58: 0.060161545275697385

#[INFO ] 2019-04-03 03:20:03.922 [main] TuningEpdReader - result [-1, 1, 0, 2, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, -1, 1, 1, 1, 0, 1, -1, 2, 0, 1, 0, 0, -1, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, -1, 1, 1, 1, 0, -1, 1, -2, 1, 0, 0, -2, 1, 0, 0, -2, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, -1, 0, 0, 1, 1, 0, 1, 0, -1, 1, 1, 1, 1, 0, 0, 0, 0, 0, -2, 0, 0, 0, 0, 0, -1, 1, 1, 1, -1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, -1, 0, 1, 0, -1, 0, 2, 2, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 2, 1, 0, 0, 2, 0, 2, 1, 0, 1, 1, 2, 0, 1, 2, 0, 0, 1, 1, 0, 0, -2, 0, 1, 1, -2, -1, 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 0, 2, 2, 2, 1, 4, 1, 6, 3, 3, 3, 3, 6, 2, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, -1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1, -1, -1, -3, -1, 0, -1, -2, 0, 0, 0, 0, -1, 1, 0, -1, -1, -1, -2, -1, 0, 1, 1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, -1, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 16, 14, 11, 7, 3, -1, -5, -10, -18, 1, 1, 1, 1, 2, 0, -1, -4, 2, -7, -3, 3, 9, 10]

time in millis: 22354383 K: 1.58: 0.05985391497254735

     */

    private static final long LIMIT = 10000;
    private static final double EPSILON = 0.0000000001;
    private static final int[] WEIGHTS = {-1, 1, 0, 2, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, -1, 1, 1, 1, 0, 1, -1, 2, 0, 1, 0, 0, -1, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, -1, 1, 1, 1, 0, -1, 1, -2, 1, 0, 0, -2, 1, 0, 0, -2, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, -1, 0, 0, 1, 1, 0, 1, 0, -1, 1, 1, 1, 1, 0, 0, 0, 0, 0, -2, 0, 0, 0, 0, 0, -1, 1, 1, 1, -1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, -1, 0, 0, -1, 0, 1, 0, -1, 0, 2, 2, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 2, 1, 0, 0, 2, 0, 2, 1, 0, 1, 1, 2, 0, 1, 2, 0, 0, 1, 1, 0, 0, -2, 0, 1, 1, -2, -1, 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 0, 2, 2, 2, 1, 4, 1, 6, 3, 3, 3, 3, 6, 2, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, -1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1, -1, -1, -3, -1, 0, -1, -2, 0, 0, 0, 0, -1, 1, 0, -1, -1, -1, -2, -1, 0, 1, 1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, -1, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 16, 14, 11, 7, 3, -1, -5, -10, -18, 1, 1, 1, 1, 2, 0, -1, -4, 2, -7, -3, 3, 9, 10};

    private static Logger logger = LogManager.getLogger();

    private static double K = 1.58; // based on quiet-labeled.epd

    private static final double STEP = 0.01;

    private static ThreadLocal<Environment> environment = ThreadLocal.withInitial(Environment::new);

    private static ForkJoinPool forkJoinPool = new ForkJoinPool(4);

    private static volatile int count = 0;
    private static List<WdlFen> positions = new ArrayList<>();
    private static boolean breakHere = false;

    public static void main(String args[]) {

        String fileName = "/home/laurens/Downloads/quiet-labeled.epd";

        //read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            positions = stream
//                    .limit(LIMIT)
                    .map(TuningEpdReader::readPosition).collect(Collectors.toList());
            System.out.println(positions.size());
        } catch (IOException e) {
            logger.error("IOException reading file {}", fileName);
            logger.error("stacktrace", e);
        }
        System.out.println("W: "+positions.stream().filter(w -> w.getWdl().equals(WDL.WIN)).count());
        System.out.println("D: "+positions.stream().filter(w -> w.getWdl().equals(WDL.DRAW)).count());
        System.out.println("L: "+positions.stream().filter(w -> w.getWdl().equals(WDL.LOSS)).count());

        new Thread(() -> {
            System.out.println("press any key to stop");
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            breakHere = true;
        }).start();
        long millis = System.currentTimeMillis();
//        determineK(positions);


        int[] weights = new int[Evaluator.PARAMETER_COUNT];//{0, 0, 0, -1, 0, -2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -1, 0, 2, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 2, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, -1, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 2, -1, 0, 0, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0};
//        weights[0]--;


//        time in millis: 12791770 K: 1.58: 0.060345966723170635

//                new int[403];//{0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1};

//        time in millis: 1430549 K: 1.58: 0.060344782936697844

//        new int[403];

//        double error = calculateError(weights);

        logger.info("initial {}", Arrays.toString(weights));
        int[] result = localOptimize(weights);
        logger.info("result {}", Arrays.toString(result));
        double error = calculateError(result);
        System.out.println("\ntime in millis: " + (System.currentTimeMillis()-millis) + " K: "+ K+": "+error);
    }

    private static int[] localOptimize(int[] initial){
        long millis = System.currentTimeMillis();
        final int nParams = initial.length;
        double bestError = calculateError(initial);
        int[] bestWeights = Arrays.copyOf(initial, initial.length);
        boolean improved = true;
        System.out.println("starting error:  " + bestError);
//        System.out.print("improving    ");
        while(improved){
            improved = false;
            Queue<PreviousSuccess> pq = new PriorityQueue<>();
            for (int pi = 0; pi<nParams; pi++){
                int[] toTest = Arrays.copyOf(bestWeights, bestWeights.length);
                toTest[pi]+=1;
                double newError = calculateError(toTest);
                if (newError < bestError){
                    double improvement = bestError-newError;
                    if (improvement>EPSILON) {
                        pq.add(new PreviousSuccess(improvement, pi, 1));
                        bestError = newError;
                        bestWeights = Arrays.copyOf(toTest, toTest.length);
                        improved = true;
                        logger.info("\n{}\t{} improved {} to {} (+1)", bestError, Duration.ofMillis(System.currentTimeMillis() - millis), pi, toTest[pi]);
                    }
                } else {
                    toTest[pi]-=2;
                    newError = calculateError(toTest);
                    if (newError < bestError) {
                        double improvement = bestError-newError;
                        if (improvement>EPSILON) {
                            pq.add(new PreviousSuccess(improvement, pi, -1));
                            bestError = newError;
                            bestWeights = Arrays.copyOf(toTest, toTest.length);
                            improved = true;
                            logger.info("\n{}\t{} improved {} to {} (-1)", bestError, Duration.ofMillis(System.currentTimeMillis() - millis), pi, toTest[pi]);
                        }
                    }
                }
                if (breakHere) break;
            }
            if (breakHere) break;
            System.out.println(pq.size());
            while (!pq.isEmpty()){
                PreviousSuccess ps = pq.poll();
                System.out.println(ps);
                int pi = ps.getFeature();
                int[] toTest = Arrays.copyOf(bestWeights, bestWeights.length);
                toTest[pi]+=ps.getStep();
                double newError = calculateError(toTest);
                if (newError < bestError) {
                    double improvement = bestError - newError;
                    if (improvement > EPSILON) {
                        pq.add(new PreviousSuccess(improvement, pi, ps.step));
                        bestError = newError;
                        bestWeights = Arrays.copyOf(toTest, toTest.length);
                        logger.info("\n{}\t{} improved {} to {} ({}) {} left", bestError, Duration.ofMillis(System.currentTimeMillis() - millis), pi, toTest[pi], ps.getStep(), pq.size());
                    }
                } else {
                    logger.info("{} left", pq.size());
                }
                if (breakHere) break;
            }
        }
        return bestWeights;
    }

    private static double calculateError(int[] weights){
        try {
            Evaluator.applyWeights(weights);
            return forkJoinPool.submit(
                    () -> positions.parallelStream()
                            .collect(Collectors.averagingDouble(TuningEpdReader::valuationError))
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn(e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static double determineK(List<WdlFen> positions) {
        double newAvgError, bestAvgError = 0;
        boolean reversed = false;
        while (true) {
            count = 0;
            newAvgError = positions.stream().peek(TuningEpdReader::debug).collect(Collectors.averagingDouble(TuningEpdReader::valuationError));
            System.out.println("\nK:"+ K +" "+ newAvgError);
            if (bestAvgError == 0 || newAvgError < bestAvgError){
                bestAvgError = newAvgError;
                if (reversed){
                    K = K - STEP;
                } else {
                    K = K + STEP;
                }
            } else if (!reversed){
                reversed = true;
                K = K - STEP*2;
            } else {
                K = K + STEP;
                System.out.println("BEST K: " + K + "(avg error: "+ bestAvgError +")");
                break;
            }
        }
        return K;
    }

    private static void debug(WdlFen p) {
        count++;
        if (count%25000==0){
            System.out.print("_");
        }
        if (count%100000==0){
            System.out.print("I");
        }
    }


    static double valuationError(WdlFen wdlFen){
        int valuation;
//        if (wdlFen.getValuation()==null){
        valuation = valuate(wdlFen.getFen());
//            wdlFen.setValuation(valuation);
//        } else {
//            valuation = wdlFen.getValuation();
//        }
        return Math.pow(wdlFen.getWdl().getValue() - sigmoid(valuation), 2d);
    }

    static double sigmoid(double valuation){
        double power = -K * valuation/400d;
        return 1/(1+Math.pow(10, power));
    }

    static int valuate(String fen) {
        try {
            environment.get().setupBoard(fen);
            return environment.get().getQScore();
        } catch (IllegalFENException e) {
            logger.warn("illegal fen {}", fen);
        }
        return 0;
    }

    /**
     * reads a line of input or just any epd line and returns an EPD object
     * @param input the epd line
     * @return the EPD object
     */
    public static WdlFen readPosition(String input){
        String[] substrings = input.split(" ");
        String fen = substrings[0] + " " + substrings[1] + " "+ substrings[2] + " " + substrings[3];
        String rest = substrings[4];
        for (int i = 5; i<substrings.length;i++){
            rest = rest + " " + substrings[i];
        }
        String result = "";
        String[] restSubstrings = rest.split(";");
        for (String restSubstring : restSubstrings){
            String[] commandSubstring = restSubstring.trim().split(" ");
            if (commandSubstring[0].trim().equalsIgnoreCase("c1")){
                result = commandSubstring[1].trim();
            } else if (commandSubstring[0].trim().equalsIgnoreCase("c9")){
                result = commandSubstring[1].trim().replaceAll("\"","");
            }
        }

        return new WdlFen(fen, result);
    }

    public List<WdlFen> readAll() {
        String fileName = "/home/laurens/Downloads/quiet-labeled.epd";

        //read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            return stream
                    .map(TuningEpdReader::readPosition).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("IOException reading file {}", fileName);
            logger.error("stacktrace", e);
            return new ArrayList<>();
        }

    }

    public static void displayNewValues() {
        Evaluator.applyWeights(WEIGHTS);
        Map<String, Object> parameterMap = Evaluator.getParameters();
        for (String parameterName : parameterMap.keySet()){
            String type = null;
            if (parameterMap.get(parameterName) instanceof int[]){
                int[] array = (int[]) parameterMap.get(parameterName);
                System.out.println(String.format("private static int[] %s = {%s};", parameterName, Arrays.toString(array).replaceAll("\\[|\\]","")));
            } else if (parameterMap.get(parameterName) instanceof Integer){
                int primitive = (int) parameterMap.get(parameterName);
                System.out.println(String.format("private static int %s = %d;", parameterName, primitive));
            }
        }
    }

    private static class PreviousSuccess implements Comparable<PreviousSuccess>{
        private double errorImprovement;
        private int feature;
        private int step;

        public int getStep() {
            return step;
        }

        public int getFeature() {
            return feature;
        }

        public PreviousSuccess(double errorImprovement, int feature, int step) {
            this.errorImprovement = errorImprovement;
            this.feature = feature;
            this.step = step;
        }

        @Override
        public int compareTo(PreviousSuccess o) {
            return -Double.compare(errorImprovement, o.errorImprovement);
        }

        @Override
        public String toString() {
            return "PreviousSuccess{" +
                    "errorImprovement=" + errorImprovement +
                    ", feature=" + feature +
                    ", step=" + step +
                    '}';
        }
    }
}