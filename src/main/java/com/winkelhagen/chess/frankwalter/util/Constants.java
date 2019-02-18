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
package com.winkelhagen.chess.frankwalter.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class Constants {// NOSONAR
    public static final Logger logger = LogManager.getLogger();

    private static Properties frankwalterProperties = new Properties();
    private static Properties gitProperties = new Properties();

    static {
        try {
            frankwalterProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("frankwalter.properties"));
            gitProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("git.properties"));
        } catch (IOException e) {
            logger.warn("unable to load property-files", e);
        }
    }

    public static final int COMPLEX_EVAL_MARGIN = 300;
    /* constants */
    public static final int EMPTY = 0;
    public static final int ALL = 0;
    public static final int QUEEN = 1;
    public static final int ROOK = 2;
    public static final int BISHOP = 3;
    public static final int KNIGHT = 4;
    public static final int KING = 5;
    public static final int PAWN = 6;
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    public static final int NO_SQUARE = -1;
    public static final int MULTIPLE_SQUARES = 64;

    public static final int SCORE_DROP_PANIC_THRESHOLD = 50;

    public static final boolean SAVE_BEST_FAIL_LOW = false;

    public static final boolean LAZY_EVAL = true;

    public static final boolean TT_NARROWS_BOUNDS = true;

    //http://talkchess.com/forum3/viewtopic.php?f=7&t=47373 - so it might improve with a bucket system voor TT.
    public static final boolean TT_IN_QSEARCH = true;

    public static final boolean QUICK_ASPIRATION_RESEARCH = true;

    private static final String ENGINE_NAME = "FrankWalter";
    private static final String VERSION_DEFAULT = "version";
    private static final String REVISION_DEFAULT = "revision";
    public static final String AUTHORNAME = "Laurens Winkelhagen";
    public static final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final boolean USE_TB = true;

    public static String getVersion(){
        return frankwalterProperties.getProperty("frankwalter.version.number", VERSION_DEFAULT);
    }
    private static String getRevision() {
        return gitProperties.getProperty("git.commit.id.abbrev", REVISION_DEFAULT);
    }

    public static String getEngineName() {
        String version = Constants.getVersion();
        if (version.endsWith("-SNAPSHOT")){
            return String.format("%s %s-%s", Constants.ENGINE_NAME, version.substring(0, version.indexOf('-')), Constants.getRevision());
        } else {
            return String.format("%s %s", Constants.ENGINE_NAME, Constants.getVersion());
        }
    }


}
