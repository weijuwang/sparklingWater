object Card {
    /**
     * A face-down card that we may or may not know. Unknown cards are [Unknown]s; known cards are [Known]s.
     */
    interface PossiblyUnknown

    /**
     * A card and its associated properties.
     *
     * An unknown card, when valid, is always represented as [Unknown].
     */
    enum class Known(val abbreviation: String, val points: Int) : PossiblyUnknown {
        ACE("A", 1),
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        SEVEN("7", 7),
        EIGHT("8", 8),
        NINE("9", 9),
        TEN("10", 10),
        JACK("J", 10),
        QUEEN("Q", 10),
        BLACK_KING("BK", 10),
        RED_KING("RK", -1),
        JOKER("0", 0);
    }

    /**
     * An unknown card.
     */
    class Unknown : PossiblyUnknown
}