/**
 *
 */
enum class State(
    /**
     * If `true`, any player may stick the most recent card in the discard pile from this state.
     */
    val stickable: Boolean = false,

    /**
     * Some after-discard states have an action that a player may take, like [AFTER_DISCARD_78] and [Action.PeekAtOwnCardAs0].
     * These actions are always optional and are marked with [optional]=true, which will make [Action.SkipAction] legal.
     */
    val optional: Boolean = false
) {
    /**
     * The beginning of a turn.
     */
    BEGINNING_OF_TURN,

    /**
     * The state after drawing a card.
     */
    AFTER_DRAW,

    /**
     * The state after discarding a 7 or 8.
     */
    AFTER_DISCARD_78(stickable=true, optional=true),

    /**
     * The state after discarding a 9 or 10.
     */
    AFTER_DISCARD_910(stickable=true, optional=true),

    /**
     * The state after discarding a face card.
     */
    AFTER_DISCARD_FACE(stickable=true, optional=true),

    /**
     * The state after discarding a black king.
     */
    AFTER_DISCARD_BLACK_KING(stickable=true, optional=true),

    /**
     * The state after discarding a black king and peeking at a card.
     */
    AFTER_PEEK_BLACK_KING(stickable=true,optional=true),

    /**
     * The end of a turn, after a discard or swap has been done and all special actions resolved.
     */
    END_OF_TURN(stickable=true),

    /**
     * The end of the game.
     */
    END_OF_GAME;
}