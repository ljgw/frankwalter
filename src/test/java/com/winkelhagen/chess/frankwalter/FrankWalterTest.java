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

import com.winkelhagen.chess.frankwalter.ci.OutputPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FrankWalterTest {

    //explicitly not using a book
    private static final String[] ARGS = {"-nobook"};
    private InputStream originalStdin;
    private PrintStream originalStdout;
    private PipedOutputStream testStdin;
    private BufferedWriter input;
    private PipedInputStream output = new PipedInputStream();
    private BlockingQueue<String> outputQueue = new ArrayBlockingQueue<>(1000);
    private Thread gameThread;

    @Before
    public void setup() throws IOException {
        //divert io streams
        originalStdout = System.out;
        PrintStream testStdout = new PrintStream(new PipedOutputStream(output), true);
        OutputPrinter.setOutputPrintStream(testStdout);
        originalStdin = System.in;
        testStdin = new PipedOutputStream();
        System.setIn(new PipedInputStream(testStdin));
        input = new BufferedWriter(new OutputStreamWriter(testStdin));
        outputQueue.clear();
        Thread outputThread = new Thread(() -> new Scanner(output).useDelimiter(System.lineSeparator()).forEachRemaining(s -> {
            System.err.println("output: " + s);
            if (!s.startsWith("#")){
                assertTrue(outputQueue.offer(s));
            }
        }));
        outputThread.setDaemon(true);
        outputThread.start();

        //start game thread
        gameThread = new Thread(() -> FrankWalter.main(ARGS));
        gameThread.start();

    }

    @After
    public void teardown() throws InterruptedException {
        //revert streams
        System.setIn(originalStdin);
        OutputPrinter.setOutputPrintStream(originalStdout);

        //end game thread
        System.err.println("test: ending test; waiting at most one second for the engine to shutdown");
        long millis = System.currentTimeMillis();
        gameThread.join(1000);
        System.err.println("test: ending test; waited " + (System.currentTimeMillis() - millis) + " milliseconds");
        assertFalse("game thread should be dead now", gameThread.isAlive());
    }

    @Test
    public void testAFewMoves() throws Exception {


        System.err.println("test: starting test");
        send("xboard");
        send("protover 2");
        waitFor("^feature done=1$");
        send("force");
        send("new");
        send("random");
        send("level 40 5 0");
        send("post");
        send("hard");
        send("force");
        send("time 30000");
        send("otim 30000");
        send("usermove e2e4");
        send("go");
        waitFor("^6.*");
        send("?");
        waitFor("^move.*");
        send("usermove d2d4");
        waitFor("^6.*");
        send("?");
        waitFor("^move.*");
        clearOutputQueue();

        send("quit");

    }

    private void clearOutputQueue() throws InterruptedException {
        while (true){
            String outputReceived = outputQueue.poll(1, TimeUnit.SECONDS);
            if (outputReceived == null){
                System.err.println("test: OutputQueue Cleared");
                break;
            }
        }
    }

    private void waitFor(String regex) throws InterruptedException {
        while (true){
            String outputReceived = outputQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull("no output for 1 second", outputReceived);
            if (outputReceived.matches(regex)){
                System.err.println("test: received " + regex);
                break;
            }
        }
    }

    private void send(String command) throws IOException {
        System.err.println("input: " + command);
        input.append(command);
        input.newLine();
        input.flush();
    }

}