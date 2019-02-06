FrankWalter
============

FrankWalter ${version} is a winboard/xboard chess engine written in Java.

Usage
-----
FrankWalter is a java chess engine that requires java 8 or higher to work. Java can be downloaded from https://www.java.com/.
The engine can be started with the command `java -jar frankwalter.jar <options>` where `<options>` are optional commandline parameters.

Currently the following commandline parameters are available:

* `-debug` (no arguments, causes logging to be stored in a file called `debug.log` in the current working directory)
* `-nobook` (no arguments, tells the engine not to use its own book)
* `-book <bookname>` (`<bookname>` should be the filename of an openingbook in Beowulf format, located next to the .jar file. The default is `frankwalter.opening` which is included in the engine itself)
* `-tt <size>` (where `<size>` is the size is the size of the TranspositionTable in mb or gb. Possible values are 1mb, 2mb, 4mb, 8mb, 16mb, 32mb, 64mb, 128mb, 256mb, 512mb, 1gb. The default is 256mb)
* `-tb <tablebases filepath>` (here `<tablebases filepath>` is the Syzygy-tablebases directory. It is also possible to configure this in xboard)

Features
--------
* Bitboard legal move generator using magic bitboards and kindergarten bitboards
* Principal Variation Search with aspiration window.
* Mostly Piece-Square value based evaluation (tuned using texels tuning method)
* Easy to understand opening-book format (beowulf format: use your own by adding a frankwalter.openings file next to the .jar file)
* Syzygy Tablebases support (dependant on platform: I have only compiled the native code for linux and windows)

Syzygy Tablebases
-----------------
This java program makes use of Syzygy Tablebases via the Java Syzygy-Bridge: a modification of the Fathom c program, in combination with a java library that can use this native library. Due to practical constraints there is no apple macintosh version of the JSyzygy library available. In order to use Syzygy tablebases, the file libJSyzygy.so (linux) or JSyzygy.dll (windows) should be placed next to the frankwalter.jar file, or in a java library directory. The Syzygy tablebases themselves can be generated locally, or downloaded from the internet.

It should be possible to compile the JSyzygy library on MacOs (and name it libJSyzygy.jnilib I suppose), but I don't have the means nor the experience to test this myself. The JSyzygy library can be compiled of my fork of basil00's Fathom program, located in github: https://github.com/ljgw/Fathom.

Acknowledgements
----------------
Frank-Walter would not have been complete or even possible without the inspiration and material provided by the following people:

* Working with magic bitboards and kindergarten bitboards it is hard not to acknowledge Gerd Isenberg, Pradu Kannan en Lasse Hansen.
* The openingbook format is the Beowulf-format. Principal author Colin Frayn also provided the basic book, used with permission.
* Texels Tuning Method was developed by Peter Österlund, I have made use of the positions provided by the authors of Zurichess (quiet-labeled.epd)
* Syzygy Tablebases were developed by Ronald de Man. Basil00 made a c/c++ library for easy integration in c/c++ projects. I've taken the fork of Jon Dart and integrated in my Java program via JNI.
* In general, the people on TalkChess forums and behind the Chessprogramming.org wiki have been great sources of ideas and knowledge.
* Finally Ron Murawski for hosting versions of FrankWalter on http://www.computer-chess.org.
