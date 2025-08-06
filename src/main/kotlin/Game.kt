import Card.Known.*

/**
 * TODO Reset draw pile when empty
 *
 * Represents the state of a Cambio game.
 */
interface Game {
    /**
     * The number of players in the game.
     */
    val numPlayers: Int
    
    /**
     * The turn counter.
     */
    var turn: Int

    /**
     * Cards in the discard pile.
     */
    val discardPile: MutableList<Card.Known>

    /**
     * The last drawn card.
     */
    var drawnCard: Card.MaybeKnown

    /**
     * The player who called Cambio, if any.
     */
    var cambioCaller: Int?

    /**
     * `true` if the previously discarded card has been stuck.
     */
    var stuck: Boolean

    /**
     * An enum indicating the state of the game. See [State] for more.
     */
    var state: State

    /**
     * The list of [Action]s that have been made in this game.
     */
    val actionHistory: MutableList<Action>

    /**
     * Increments the turn, overflowing back to 0 if necessary.
     */
    fun incTurn() {
        turn = if (turn == numPlayers - 1)
            0
        else
            turn + 1
    }

    /**
     * A game from player 0's perspective. Cards unknown to that player are represented as [Card.Unknown].
     *
     * @param numPlayers The number of players in the game.
     * @param firstPlayer The index of the first player.
     * @param jokers If `true`, 2 jokers are inserted into the deck.
     * @param bottomLeftCard The bottom-left card revealed to player 0 at the beginning of the game, stored in `playerCardInfo[0][2]`.
     * @param bottomRightCard The bottom-right card revealed to player 0 at the beginning of the game, stored in `playerCardInfo[0][3]`.
     */
    class PartialInfo(
        override val numPlayers: Int,
        firstPlayer: Int,
        jokers: Boolean,
        bottomLeftCard: Card.Known,
        bottomRightCard: Card.Known
    ) : Game {
        override var turn = firstPlayer

        /**
         * Face-down cards whose values are unknown to us. This includes all cards in the draw pile and any players'
         * cards we haven't seen. Once we see a card, it is moved to [playerCardInfo].
         */
        val unseenCards = mapOf(
            ACE to 4, TWO to 4, THREE to 4, FOUR to 4, FIVE to 4,
            SIX to 4, SEVEN to 4, EIGHT to 4, NINE to 4, TEN to 4,
            JACK to 4, QUEEN to 4, BLACK_KING to 2, RED_KING to 2,
            JOKER to if (jokers) 2 else 0
        )
            .flatMap { (card, freq) -> List(freq) { card } }
            .toMutableList() - bottomLeftCard - bottomRightCard

        override val discardPile = mapOf(
            ACE to 0, TWO to 0,
            THREE to 0,
            FOUR to 0,
            FIVE to 0,
            SIX to 0,
            SEVEN to 0,
            EIGHT to 0,
            NINE to 0,
            TEN to 0,
            JACK to 0,
            QUEEN to 0,
            BLACK_KING to 0,
            RED_KING to 0,
            JOKER to 0
        )
            .flatMap { (card, freq) -> List(freq) { card } }
            .toMutableList()

        /**
         * Size of the draw pile.
         */
        // Add 2 back because they were removed from [unseenCards] already
        var drawPileSize = 2 + unseenCards.size - 4 * numPlayers

        /**
         * Information we have on each players' cards. As an example, card 0 of player 3 is `playerCards[3][0].`
         */
        val playerCardInfo: Array<MutableList<Card.MaybeKnown>> =
            arrayOf(
                mutableListOf(
                    Card.Unknown(), Card.Unknown(),
                    bottomLeftCard, bottomRightCard
                )
            ) + Array(numPlayers - 1) { MutableList(4) { Card.Unknown() } }

        override var drawnCard: Card.MaybeKnown = Card.Unknown()
        override var cambioCaller: Int? = null
        override var stuck = false
        override var state = State.BEGINNING_OF_TURN
        override val actionHistory = mutableListOf<Action>()
    }

    /**
     * A determinized game state with all cards known.
     *
     * Given an actual [game], the constructor will populate unknown cards with possible values. Therefore, a
     * [Determinized] game represents one possible perfect-information state of the game represented by [game].
     *
     * @param game The actual game from which to initialize a hypothetical state.
     */
    class Determinized(game: PartialInfo) : Game {
        override val numPlayers = game.numPlayers
        override var turn = game.turn
        override val discardPile = game.discardPile
            // Copy
            .toMutableList()

        /**
         * List of cards in the draw deck.
         *
         * Use [draw] to draw a card.
         */
        val drawPile = game.unseenCards
            .shuffled()
            // Copy
            .toMutableList()

        /**
         * The cards that each player has.
         */
        val playerCards = game.playerCardInfo
            .map {
                it.map { card ->
                    card as? Card.Known ?: draw()
                }
                    .toMutableList()
            }

        override var drawnCard: Card.MaybeKnown = game.drawnCard as? Card.Known ?: draw()
        override var cambioCaller = game.cambioCaller
        override var stuck = game.stuck
        override var state = game.state
        override val actionHistory = game.actionHistory
            // Copy
            .toMutableList()

        /**
         * Draws a card from the draw pile, assuming it has already been shuffled.
         */
        fun draw() = drawPile.removeLast()

        /**
         * Returns all legal actions.
         */
        fun legalActions() = buildSet {
            when(state) {

                State.BEGINNING_OF_TURN -> {
                    add(
                        if (turn == 0)
                            Action.DrawAs0
                        else
                            Action.DrawNotAs0
                    )
                    if (cambioCaller == null)
                        add(Action.Cambio)
                }

                State.AFTER_DRAW -> {
                    add(
                        if (turn == 0)
                            Action.DiscardAs0
                        else
                            Action.DiscardNotAs0
                    )

                    addAll(
                        playerCards[turn].indices
                            .map { Action.Swap(it) }
                    )
                }

                State.AFTER_DISCARD_78 -> addAll(
                    playerCards[turn].indices
                        .map {
                            if (turn == 0)
                                Action.PeekAtOwnCardAs0(it)
                            else
                                Action.PeekAtOwnCardNotAs0(it)
                        }
                )

                State.AFTER_DISCARD_910, State.AFTER_DISCARD_BLACK_KING -> addAll(
                    (1..<numPlayers)
                        .flatMap { player ->
                            playerCards[player].indices
                                .map {
                                    if (turn == 0)
                                        Action.PeekAtOtherCardAs0(player, it)
                                    else
                                        Action.PeekAtOtherCardNotAs0(player, it)
                                }
                        }
                )

                State.AFTER_DISCARD_FACE -> addAll(
                    // Iterating through all combinations of two players
                    (0..<numPlayers)
                        .flatMap { playerA -> // Select first player
                            ((playerA + 1)..<numPlayers)
                                .flatMap { playerB -> // Select second player
                                    playerCards[playerA].indices
                                        .flatMap { indexA -> // Select first player's card
                                            playerCards[playerB].indices
                                                .map { indexB -> // Select second player's card
                                                    Action.BlindSwitch(playerA, indexA, playerB, indexB)
                                                }
                                        }
                                }
                        }
                )

                State.AFTER_PEEK_BLACK_KING -> addAll(
                    playerCards[turn].indices
                        .map { Action.BlackKingSwitch(it) }
                )

                State.END_OF_TURN -> add(Action.EndTurn)

                State.END_OF_GAME -> return@buildSet
            }

            if(state.optional)
                add(Action.SkipAction)

            if(!stuck && state.stickable)
                add(TODO("Generate list of possible sticks. Explain in comment that we don't consider false sticks"))
        }

        /**
         * Determines the winners of the game if it were scored right now.
         */
        fun winners(): List<Int> {
            val scores = playerCards
                .map { cards ->
                    cards.sumOf { it.points }
                }
            val winningScore = scores.min()
            val tiedScores = (0..<numPlayers)
                .filter {
                    scores[it] == winningScore
                }
            return if(tiedScores.size == 1)
                tiedScores
            else
                tiedScores
                    .filter { it != cambioCaller }
        }
    }
}