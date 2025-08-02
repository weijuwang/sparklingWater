/**
 * Keeps track of the current turn in a game, cycling back to 0 (the first player) after the last player has made their turn.
 *
 * Use [toInt] to access the value.
 *
 * Use the `++` operator to increment the turn. The logic of cycling back to 0 is handled automatically.
 */
class TurnCounter(val numPlayers: Int, val firstTurn: Int) {
    /**
     * The player whose turn it is to play.
     */
    private var turn = firstTurn

    /**
     * Returns a turn counter indicating the next player to move.
     */
    operator fun inc() =
         TurnCounter(numPlayers, if (turn == numPlayers - 1) 0 else turn + 1)

    fun toInt() = turn
}