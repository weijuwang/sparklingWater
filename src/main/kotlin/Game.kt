import Card.Known.*

/**
 * TODO Reset draw pile when empty
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
     * 
     */
    var cambioCaller: Int?

    /**
     * 
     */
    var stuck: Boolean

    /**
     * 
     */
    var state: State

    /**
     * The list of actions that have been made in this game.
     */
    val actionHistory: MutableList<Action>

    /**
     * Increments the turn.
     */
    fun incTurn() {
        turn = if (turn == numPlayers - 1)
            0
        else
            turn + 1
    }

    /**
     *
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
            .toMutableList()

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
        var drawPileSize = unseenCards.size - 4 * numPlayers

        /**
         * Information we have on each players' cards. As an example, card 0 of player 3 is `playerCards[3][0].`
         */
        val playerCardInfo: Array<MutableList<Card.MaybeKnown>> =
            arrayOf(
                mutableListOf(
                    Card.Unknown(),
                    Card.Unknown(),
                    bottomLeftCard,
                    bottomRightCard
                )
            ) + Array(numPlayers - 1) { MutableList(4) { Card.Unknown() } }

        override var drawnCard: Card.MaybeKnown = Card.Unknown()
        override var cambioCaller: Int? = null
        override var stuck = false
        override var state = State.BEGINNING_OF_TURN
        override val actionHistory = mutableListOf<Action>()
    }

    /**
     *
     */
    class Determinized(game: PartialInfo) : Game {
        override val numPlayers = game.numPlayers
        override var turn = game.turn
        override val discardPile = game.discardPile
            // Copy
            .toMutableList()

        /**
         * 
         */
        val drawPile = game.unseenCards
            // Copy
            .toMutableList()

        /**
         * 
         */
        val playerCards = game.playerCardInfo.map { it.map { card ->
            card as? Card.Known ?: drawRandom()
        }.toMutableList() }

        override var drawnCard = game.drawnCard
        override var cambioCaller = game.cambioCaller
        override var stuck = game.stuck
        override var state = game.state
        override val actionHistory = game.actionHistory
            // Copy
            .toMutableList()

        /**
         * Draws a random card and removes it from the [drawPile].
         */
        fun drawRandom() = drawPile.removeAt((0..<drawPile.size).random())

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
                    if (cambioCaller != null)
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
                        .map { Action.PeekAtOwnCardAs0(it) }
                )

                State.AFTER_DISCARD_910, State.AFTER_DISCARD_BLACK_KING -> addAll(
                    (1..<numPlayers)
                        .flatMap { player ->
                            playerCards[player].indices
                                .map { Action.PeekAtOtherCardAs0(player, it) }
                        }
                )

                State.AFTER_DISCARD_FACE -> addAll(
                    // Iterating through all combinations of two players
                    (1..<numPlayers)
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
                        .map { Action.BlackKingSwap(it) }
                )

                State.END_OF_TURN -> add(Action.EndTurn)

                State.END_OF_GAME -> return@buildSet
            }

            if(state.optional)
                add(Action.SkipAction)

            if(!stuck && state.stickable)
                add(TODO("Generate list of possible sticks. Explain in comment that we don't consider false sticks"))
        }
    }
}