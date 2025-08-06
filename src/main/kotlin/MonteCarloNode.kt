import kotlin.math.*

/**
 *
 */
class MonteCarloNode private constructor(
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
    private val children: MutableMap<Action, MonteCarloNode> = mutableMapOf(),

    /**
     * Parent node
     */
    val parent: MonteCarloNode? = null
) {
    /**
     * Initializes a fresh MCTS tree. [player] is automatically initialized as -1 because this node, unlike all child nodes, does not represent an action.
     */
    constructor() : this(player=-1)

    companion object {
        /**
         * The UCT exploration parameter.
         */
        private const val EXPLORATION_PARAMETER = 1.414
    }

    /**
     * Access a child node. `null` if the child doesn't exist.
     */
    operator fun get(action: Action) =
        children[action]

    /**
     * Evaluate the UCT function on this node.
     */
    private fun uct() =
        wins/playouts + EXPLORATION_PARAMETER * sqrt(
            ln(parent!!.playouts.toDouble())
                    / playouts
            )

    /**
     * Recursively executes the selection and expansion phase, returning the newly expanded node and updating [determinized] with the actions that were made.
     */
    private fun selectAndExpand(determinized: Game.Determinized): MonteCarloNode {
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

        // Execute the action
        expandedAction.execute(determinized)

        // Create the node
        children[expandedAction] = MonteCarloNode(
            player=when (expandedAction) {
                is Action.Stick -> expandedAction.player
                is Action.StickAndGiveAway -> expandedAction.stickPlayer
                else -> determinized.turn
            },
            parent=this
        )

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
        if(player in winners)
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
}
