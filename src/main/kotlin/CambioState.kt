import Card.Known.*

/**
 *
 */
class CambioState(
    numPlayers: Int,
    firstPlayer: Int,
    jokers: Boolean,
    bottomLeftCard: Card.Known,
    bottomRightCard: Card.Known
) {
    /**
     * The turn counter.
     */
    val turn = TurnCounter(numPlayers, firstPlayer)

    /**
     * Face-down cards whose values are unknown to us. This includes all cards in the draw pile and any players'
     * cards we haven't seen. Once we see a card, it is moved to [playerCards].
     */
    val unseenCards: Map<Card.Known, Int> = mapOf(
        ACE to 4, TWO to 4, THREE to 4, FOUR to 4, FIVE to 4,
        SIX to 4, SEVEN to 4, EIGHT to 4, NINE to 4, TEN to 4,
        JACK to 4, QUEEN to 4, BLACK_KING to 2, RED_KING to 2,
        JOKER to if(jokers) 2 else 0
    )

    /**
     * Cards in the discard pile.
     */
    val discardedCards: Map<Card.Known, Int> = mapOf(
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

    /**
     * Size of the draw deck.
     */
    val drawDeckSize = unseenCards.values.sum() - 4 * numPlayers

    /**
     * Information we have on each players' cards. Unknown cards are represented by `null`. As an example,
     * card 0 of player 3 is `playerCards[3][0].`
     *
     * Use the methods of [CardGrid] to modify players' cards.
     */
    val playerCards = arrayOf(CardGrid(listOf(Card.Unknown(), Card.Unknown(), bottomLeftCard, bottomRightCard))) +
            Array(numPlayers - 1) { CardGrid() }
}