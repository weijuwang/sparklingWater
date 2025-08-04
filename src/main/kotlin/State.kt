/**
 *
 */
enum class State(val stickable: Boolean = false) {
    /**
     * The beginning of a turn.
     */
    TURN_BEGIN,

    /**
     * The state after drawing a card.
     */
    AFTER_DRAW,

    /**
     * The state after discarding a 7 or 8.
     */
    AFTER_DISCARD_78(stickable=true),

    /**
     * The state after discarding a 9 or 10.
     */
    AFTER_DISCARD_910(stickable=true),

    /**
     * The state after discarding a face card.
     */
    AFTER_DISCARD_FACE(stickable=true),

    /**
     * The state after discarding a black king.
     */
    AFTER_DISCARD_BLACK_KING(stickable=true),

    /**
     *
     */
    AFTER_PEEK_BLACK_KING(stickable=true),

    /**
     * The end of a turn, after a discard or swap has been done and all special actions resolved.
     */
    TURN_END(stickable=true),

    /**
     * The end of the game.
     *
     * No legal actions from here. This is a terminal state.
     */
    GAME_END;
}