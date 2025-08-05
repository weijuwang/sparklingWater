object Card {
    /**
     * A face-down card that we may or may not know. Unknown cards are [Unknown]s; known cards are [Known]s.
     */
    interface MaybeKnown

    /**
     * A card and its associated properties.
     *
     * An unknown card, when valid, is always represented as [Unknown].
     */
    enum class Known(
        /**
         * A short string representing the card.
         */
        val abbreviation: String,

        /**
         * The number of points the card is worth.
         */
        val points: Int,

        /**
         * The state that a game will progress to when this card is discarded.
         */
        val nextStateWhenDiscarded: State = State.END_OF_TURN
    ) : MaybeKnown {
        ACE("A", 1),
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        SEVEN("7", 7, nextStateWhenDiscarded=State.AFTER_DISCARD_78),
        EIGHT("8", 8, nextStateWhenDiscarded=State.AFTER_DISCARD_78),
        NINE("9", 9, nextStateWhenDiscarded=State.AFTER_DISCARD_910),
        TEN("10", 10, nextStateWhenDiscarded=State.AFTER_DISCARD_910),
        JACK("J", 10, nextStateWhenDiscarded=State.AFTER_DISCARD_FACE),
        QUEEN("Q", 10, nextStateWhenDiscarded=State.AFTER_DISCARD_FACE),
        BLACK_KING("BK", 10, nextStateWhenDiscarded=State.AFTER_DISCARD_BLACK_KING),
        RED_KING("RK", -1, nextStateWhenDiscarded=State.AFTER_DISCARD_FACE),
        JOKER("0", 0);
    }

    /**
     * An unknown card.
     */
    class Unknown : MaybeKnown
}