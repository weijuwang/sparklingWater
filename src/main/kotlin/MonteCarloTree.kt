import kotlin.math.*

/**
 *
 */
data class MonteCarloTree(
    /**
     *
     */
    val wins: Int = 0,

    /**
     *
     */
    val playouts: Int = 0,

    /**
     *
     */
    val branches: Map<Action, MonteCarloTree> = mapOf(),

    /**
     *
     */
    val parent: MonteCarloTree? = null
) {
    companion object {
        /**
         *
         */
        const val EXPLORATION_PARAMETER = 1.414
    }

    /**
     *
     */
    private fun uct() =
        wins.toDouble() /playouts + EXPLORATION_PARAMETER * sqrt(
            ln(parent!!.playouts.toDouble())
                    / playouts
            )

    /**
     *
     */
    fun playout(game: Game.PartialInfo) {
        val determinized = Game.Determinized(game)
        TODO("MonteCarloTree.playout()")
        // TODO sticks should be evaluated differently
    }
}
