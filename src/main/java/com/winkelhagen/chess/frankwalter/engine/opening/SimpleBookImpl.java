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
package com.winkelhagen.chess.frankwalter.engine.opening;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.winkelhagen.chess.frankwalter.board.Board;
import com.winkelhagen.chess.frankwalter.util.IllegalFENException;
import com.winkelhagen.chess.frankwalter.util.MV;
import com.winkelhagen.chess.frankwalter.util.MersenneTwister;

public class SimpleBookImpl implements Book {
    private static Logger logger = LogManager.getLogger();
    
    private Map<String, SimpleBookEntry> book;
    private Random rnd;
    private int duplicates;

    public SimpleBookImpl(){
        book = new HashMap<>();
        rnd = new MersenneTwister();
    }

    /* (non-Javadoc)
     * @see com.winkelhagen.chess.frankwalter.engine.opening.Book#setRnd(java.util.Random)
     */
    @Override
    public void setRnd(Random rnd) {
        this.rnd = rnd;
    }
    
    /* (non-Javadoc)
     * @see com.winkelhagen.chess.frankwalter.engine.opening.Book#unloadBook()
     */
    @Override
    public void unloadBook(){
        book = new HashMap<>();
        duplicates = 0;
    }
    
    /* (non-Javadoc)
     * @see com.winkelhagen.chess.frankwalter.engine.opening.Book#probeBook(long)
     */
    @Override
    public int probeBook(long hashKey){
        if (book==null){
            return 0;
        }
        SimpleBookEntry entry = book.get(Long.toHexString(hashKey));
        if (entry==null){
            return 0;
        }
        return probeBook(entry);
    }

    private int probeBook(SimpleBookEntry entry) {
        int probabilitiesTotal = 0;
        int[] probabilities = entry.getProbabilities();
        int[] moves = entry.getMoves();
        if (moves.length!=probabilities.length){
            return 0;
        }
        for (int probability : probabilities) {
            probabilitiesTotal += probability;
        }
        int choice = rnd.nextInt(probabilitiesTotal);
        for (int i=0; i<probabilities.length; i++){
            choice-=probabilities[i];
            if (choice<0){
                return moves[i];
            }
        }
        logger.warn("bookchoice was bad! total: {}, probabilities: {}", probabilitiesTotal, Arrays.toString(probabilities));
        return 0;
    }
    
    /* (non-Javadoc)
     * @see com.winkelhagen.chess.frankwalter.engine.opening.Book#loadBook(java.lang.String)
     */
    @Override
    public boolean loadBook(String bookName){
        Board board = new Board();
        File bookFile = new File(bookName);
        boolean fileFound = true;
        InputStream in = this.getClass().getResourceAsStream(bookName);
        if (!bookFile.exists()){
            logger.info("filesystem book '{}' does not exist.", bookName);
            if (in==null) {
                return false;
            }
            fileFound = false;
        } else if (!bookFile.isFile()){
            logger.info("filesystem book '{}' is not a file.", bookName);
            if (in==null){
                return false;
            }
            fileFound = false;
        }
        duplicates = 0;
        try (BufferedReader input = new BufferedReader(getReader(fileFound, bookFile, in))){
            loadBookFromInputStream(board, input);
        } catch (IOException ioe) {
            logger.error("I/O exception occurred", ioe);
        }

        logger.info("openingbook {} read.", bookName);
        logger.info("entries: {}.", book.size());
        logger.info("duplicates: {}.", duplicates);
        
        return true;
    }

    private Reader getReader(boolean fileFound, File bookFile, InputStream in) throws FileNotFoundException {
        if (fileFound){
            return new FileReader(bookFile);
        } else {
            logger.info("using bundled book");
            return new InputStreamReader(in);
        }
    }

    @Override
    public boolean isBookMove(int move, long hashKey) {
        if (book==null){
            return false;
        }
        SimpleBookEntry entry = book.get(Long.toHexString(hashKey));
        if (entry==null){
            return false;
        }
        for (int bookMove : entry.getMoves()){
            if (move == bookMove){
                return true;
            }
        }
        return false;
    }

    private void loadBookFromInputStream(Board board, BufferedReader input) throws IOException {
        String line;
        String fen;
        int lineTypeFlag = 0;
        long hashKey = 0;
        while ((line = input.readLine()) != null && !"#END#".equalsIgnoreCase(line)) {
            if (lineTypeFlag == 0) {
                fen = line;
                hashKey = getHashFromFEN(board, fen);
                if (hashKey==0){
                    logger.debug("not storing hashKey '0' to the book, skipping position.");
                    break; // todo refactor this class, make it more robust, allow illegal positions and truly skip them.
                }
                if (book.containsKey(Long.toHexString(hashKey))){
                    logger.debug("duplicate position in opening book: {}", line);
                    duplicates++;
                }
            } else {
                SimpleBookEntry entry = new SimpleBookEntry();
                String[] variants = line.split(" ");
                int[] moves = new int[variants.length];
                int[] probabilities = new int[variants.length];
                for (int i = 0; i < variants.length; i++){
                    String[] tuple = variants[i].split("\\{");
                    moves[i] = MV.toBasicMove(tuple[0]);
                    probabilities[i] = Integer.parseInt(tuple[1].substring(0, tuple[1].length()-1));
                }
                entry.setMoves(moves);
                entry.setProbabilities(probabilities);
                book.put(Long.toHexString(hashKey), entry);
            }
            lineTypeFlag^=1;
        }
    }

    private long getHashFromFEN(Board board, String fen) {
        long hashKey;
        try {
            board.setupBoard(fen);
            hashKey = board.getHashKey();
        } catch (IllegalFENException ife){
            hashKey = 0;
            logger.warn("illegal position in openingbook: {}", fen, ife);
        }
        return hashKey;
    }
    
}
