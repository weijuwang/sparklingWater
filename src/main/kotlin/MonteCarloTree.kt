/**
 *
 */
data class MonteCarloTree(
    val wins: Int = 0,
    val playouts: Int = 0,
    val branches: Map<Action, MonteCarloTree> = mapOf()
) {
    /**
     *
     */
    fun playout(game: Game.PartialInfo) {
        val determinized = Game.Determinized(game)
        TODO("MonteCarloTree.playout()")
        // TODO sticks should be evaluated differently
    }
}
