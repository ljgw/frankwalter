##FrankWalter - history
The latest version of FrankWalter is ${version}

####v2.3.5-SMP
* SMP
####v2.3.5
* LMR
* fixed a some consistency bugs in doMove/undoMove
* fixed a bug in TranspositionTable
* saving the bestMove for hashMove also when failing low to show a longer PV
####v2.3.4
* Time management:
  * try to finish an iteration before returning best move
  * when the score drops use more of the maximum allotted time
####v2.3.3
* TT in buckets (64 bytes per bucket = 4 slots)
* bugfix in session based timecontrols
####v2.3.2
* improved speed starting a game
* fixed some thinking-time / ponder bugs
####v2.3.1
* Major overhaul of the user interface code (still xboard)
* Some bugfixes on pondering
* Some code cleanup in order to open source FrankWalter