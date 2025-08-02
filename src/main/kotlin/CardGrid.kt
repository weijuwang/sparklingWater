/**
 * A [CardGrid] is the set of cards a given player has in front of them.
 *
 * The initial cards are indexed 0 to 3 as follows: top left, top right, bottom left, bottom right. Our cards at
 * indices 2 and 3 are known at the beginning of the game.
 *
 * # How this representation differs from the real game
 *
 * In a real game of Cambio, players arrange their cards in a 2x2 grid and look at the bottom two, but I chose to
 * abstract this into a representation where each player has four cards and they look at the last two (indices 2 and
 * 3). This is like if they laid out their cards in a line rather than a grid, which is easier to handle in code.
 *
 * As players shed cards (or draw cards for wrong sticks), cards tend to get rearranged slightly. For example, if I
 * have my original 4 cards and stick my top right card (index 1), I now only have three cards. This does not mean
 * that there is now no card at index 1; instead we must reassign the indices to reflect that there are only 3
 * cards. Since the card at index 1 is removed, the cards at indices 2 and 3 are shifted over and there is now no
 * card at index 3.
 *
 * When transferring data between this abstract representation and a real-life game, the user must keep track of
 * which cards are which indices. A possible solution is to ask players to keep their cards in a row, but this goes
 * against how the game is traditionally played even though it does not actually affect gameplay.
 */
class CardGrid(private val cards: List<PossiblyUnknownCard> = List(4) { UnknownCard() }) {
    /**
     * Accesses the card at [index].
     */
    operator fun get(index: Int) = cards[index]

    /**
     * Returns this grid without the card at [index]. Cards at higher indices are then slotted over to fit.
     *
     * The removed card is not returned.
     */
    fun withoutCardAt(index: Int) = CardGrid(
        cards.take(index) + cards.takeLast(cards.size - index - 1)
    )

    /**
     * Returns this grid with [newCard] inserted at [index]. Cards at higher indices are moved over to fit.
     */
    fun withCardInsertedAt(index: Int, newCard: PossiblyUnknownCard) = CardGrid(
        cards.take(index) + newCard + cards.takeLast(cards.size - index)
    )

    /**
     * Returns this grid with [newCard] added to the end.
     */
    fun withCardAppended(newCard: PossiblyUnknownCard) = CardGrid(cards + newCard)

    /**
     * Returns this grid after having seen a card. This will override whatever value was there before.
     */
    fun withCardPeeked(seenCardIndex: Int, seenCard: PossiblyUnknownCard) = CardGrid(
        cards.mapIndexed { i, originalCard ->
            if (i == seenCardIndex) seenCard else originalCard
        }
    )

    companion object {
        /**
         * Returns [grids] with the cards at [indices] swapped.
         */
        fun withCardsSwapped(grids: Pair<CardGrid, CardGrid>, indices: Pair<Int, Int>): Pair<CardGrid, CardGrid> =
            Pair(
                grids.first.withCardPeeked(indices.first, grids.second[indices.second]),
                grids.second.withCardPeeked(indices.second, grids.first[indices.first])
            )
    }
}