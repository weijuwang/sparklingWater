import kotlin.math.*

object MonteCarloTreeSearch {
    /**
     *
     */
    fun search(game: Game.PartialInfo, playouts: Int): List<Pair<Action, Double>> {
        val mcts = Node(player=game.turn)
        repeat(playouts) { mcts.playout(game) }
        return mcts.children
            .map { (action, child) -> action to child.winRate() }
            .sortedByDescending { (_, value) -> value }
    }

    /**
     * A node in an MCTS tree.
     */
    private class Node(
        /**
         * The player who performed the action that leads to this node.
         */
        val player: Int,

        /**
         * Number of wins credited for this player on this node
         */
        var wins: Double = 0.0,

        /**
         * Number of playouts executed on this node
         */
        var playouts: Int = 0,

        /**
         * Child nodes
         */
        val children: MutableMap<Action, Node> = mutableMapOf(),

        /**
         * Parent node
         */
        val parent: Node? = null
    ) {
        companion object {
            /**
             * The UCT exploration parameter.
             */
            private const val EXPLORATION_PARAMETER = 1.414
        }

        fun winRate() = wins / playouts

        /**
         * Evaluate the UCT function on this node.
         */
        private fun uct() =
            winRate() + EXPLORATION_PARAMETER * sqrt(
                ln(parent!!.playouts.toDouble())
                        / playouts
            )

        /**
         * Recursively executes the selection and expansion phase, returning the newly expanded node and updating [determinized] with the actions that were made.
         */
        private fun selectAndExpand(determinized: Game.Determinized): Node {
            val legalActions = determinized.legalActions()
            val unexpandedActions = legalActions.filter { it !in children }

            if (determinized.state == State.END_OF_GAME)
                return this

            // No unexpanded actions
            if (unexpandedActions.isEmpty()) {
                return children
                    // Find child with max UCT
                    .maxBy { (_, node) -> node.uct() }
                    // Execute action
                    .also { (action, _) -> action.execute(determinized) }
                    // Get the node
                    .value
                    // Repeat recursively
                    .selectAndExpand(determinized)
            }

            /* Expand the node */

            // Select a random branch to expand
            val expandedAction = unexpandedActions.random()

            // Create the node
            children[expandedAction] = Node(
                player = when (expandedAction) {
                    is Action.Stick -> expandedAction.player
                    is Action.StickAndGiveAway -> expandedAction.stickPlayer
                    else -> determinized.turn
                },
                parent = this
            )

            // Execute the action
            expandedAction.execute(determinized)

            return children[expandedAction]!!
        }

        /**
         * Backpropagate winners of a simulation.
         * Each of the [winners] have [credit] added to nodes representing actions they were responsible for.
         *
         * ## How much [credit]?
         *
         * If there is only one winner, [credit] should always be 1.0.
         *
         * When there are multiple winners, there are several different possible scoring systems:
         * - Give 0.0 credit to anyone for a tie
         * - Give 1.0 credit to all winners who tied
         * - Share the 1.0 credit among winners. For example, if there are 3 winners, the credit is 1.0/3.
         *
         * The last option is currently implemented so that a tie is worse than a win but better than a complete loss.
         */
        private fun backpropagate(winners: List<Int>, credit: Double) {
            ++playouts
            if (player in winners)
                wins += credit
            parent?.backpropagate(winners, credit)
        }

        /**
         * Execute one playout, i.e. all four steps of the MCTS process, from [game].
         *
         * [game] is not mutated.
         *
         * See documentation on [selectAndExpand] as well as [backpropagate] for more information on those steps.
         */
        fun playout(game: Game.PartialInfo) {
            val determinized = Game.Determinized(game)

            /*
            STEPS 1 and 2: SELECTION AND EXPANSION
             */
            val expandedNode = selectAndExpand(determinized)

            /*
            STEP 3: SIMULATION
            Execute random legal actions
             */
            // TODO What if the game never ends (i.e. no one calls Cambio)?
            // TODO Probability of stick and cambio call need to be accounted for
            while (determinized.state != State.END_OF_GAME)
                determinized.legalActions()
                    .random()
                    .execute(determinized)

            /*
            STEP 4: BACKPROPAGATION
             */
            val winners = determinized.winners()
            expandedNode.backpropagate(winners, 1.0 / winners.size)
        }

        /**
         * TODO fix
         */
        fun print(maxDepth: Int = 2, indent: Int = 0) {
            repeat(indent) { print(' ') }
            for ((action, child) in children) {
                println("${child.wins}/${child.playouts} $action")
                continue
                if (indent < maxDepth)
                    child.print(maxDepth = maxDepth, indent = indent + 1)
            }
        }
    }
}