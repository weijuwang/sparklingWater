import State.*
import Card.Known.*

/**
 * TODO illegal actions
 */
interface Action {
    /**
     * An action that reveals a card.
     */
    interface CardRevealing : Action {
        /**
         * Defines the effects that should be applied to [game] after executing this action.
         */
        fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State

        /**
         * Executes this action.
         */
        fun execute(game: Game.PartialInfo, cardRevealed: Card.Known) {
            game.state = applyUniqueEffects(game, cardRevealed)
            game.actionHistory.add(this)
        }
    }

    /**
     * An action that does not reveal a card.
     */
    interface NonCardRevealing : Action {
        /**
         * Defines the effects that should be applied to [game] after executing this action.
         */
        fun applyUniqueEffects(game: Game.PartialInfo): State

        /**
         * Executes this action.
         */
        fun execute(game: Game.PartialInfo) {
            game.state = applyUniqueEffects(game)
            game.actionHistory.add(this)
        }
    }

    /**
     * Defines the effects that should be applied to [game] after executing this action.
     */
    fun applyUniqueEffects(game: Game.Determinized): State

    /**
     * Executes this action.
     */
    fun execute(game: Game.Determinized) {
        game.state = applyUniqueEffects(game)
        game.actionHistory.add(this)
    }

    /**
     *
     */
    object Draw0 : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            --game.drawPileSize
            game.drawnCard = cardRevealed
            return AFTER_DRAW
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.drawnCard = game.drawRandom()
            return AFTER_DRAW
        }
    }

    /**
     *
     */
    object DrawNon0 : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            --game.drawPileSize
            game.drawnCard = Card.Unknown()
            return AFTER_DRAW
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.drawnCard = game.drawRandom()
            return AFTER_DRAW
        }
    }

    /**
     *
     */
    object Discard0 : NonCardRevealing {
        private fun applyUniqueEffects(game: Game) =
            (game.drawnCard as Card.Known).let {
                game.discardPile.add(it)
                // Figure out which state to go to depending on what card was discarded
                when(it) {
                    SEVEN, EIGHT            -> AFTER_DISCARD_78
                    NINE, TEN               -> AFTER_DISCARD_910
                    JACK, QUEEN, RED_KING   -> AFTER_DISCARD_FACE
                    BLACK_KING              -> AFTER_DISCARD_BLACK_KING
                    else                    -> TURN_END
                }
            }

        override fun applyUniqueEffects(game: Game.PartialInfo) = applyUniqueEffects(game as Game)
        override fun applyUniqueEffects(game: Game.Determinized) = applyUniqueEffects(game as Game)
    }

    /**
     *
     */
    object DiscardNon0 : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.drawnCard = cardRevealed
            game.discardPile.add(cardRevealed)
            // Figure out which state to go to depending on what card was discarded
            return when (cardRevealed) {
                SEVEN, EIGHT -> AFTER_DISCARD_78
                NINE, TEN -> AFTER_DISCARD_910
                JACK, QUEEN, RED_KING -> AFTER_DISCARD_FACE
                BLACK_KING -> AFTER_DISCARD_BLACK_KING
                else -> TURN_END
            }
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            (game.drawnCard as Card.Known).let {
                game.discardPile.add(it)
                // Figure out which state to go to depending on what card was discarded
                return when (it) {
                    SEVEN, EIGHT -> AFTER_DISCARD_78
                    NINE, TEN -> AFTER_DISCARD_910
                    JACK, QUEEN, RED_KING -> AFTER_DISCARD_FACE
                    BLACK_KING -> AFTER_DISCARD_BLACK_KING
                    else -> TURN_END
                }
            }
        }
    }

    class Swap(val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            // TODO At this line, [cardRevealed] and [game.playerCards[game.turn][index]] should be the same. Should we throw an error if not?
            // We found out what the card is but it's getting discarded anyways, so no need to update `playerCards`
            game.discardPile.add(cardRevealed)
            // Update the newly drawn card
            game.playerCards[game.turn][index] = game.drawnCard
            return TURN_END
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.discardPile.add(game.playerCards[game.turn][index])
            game.playerCards[game.turn][index] = game.drawnCard as Card.Known
            return TURN_END
        }
    }

    /**
     *
     */
    class BlindSwitch(val playerA: Int, val indexA: Int, val playerB: Int, val indexB: Int) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            val cardA = game.playerCards[playerA][indexA]
            game.playerCards[playerA][indexA] = game.playerCards[playerB][indexB]
            game.playerCards[playerB][indexB] = cardA
            return TURN_END
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val cardA = game.playerCards[playerA][indexA]
            game.playerCards[playerA][indexA] = game.playerCards[playerB][indexB]
            game.playerCards[playerB][indexB] = cardA
            return TURN_END
        }
    }

    /**
     *
     */
    class PeekAtOwnCard(val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCards[0][index] = cardRevealed
            return TURN_END
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            // Nothing happens, we already know the card
            TURN_END
    }

    /**
     *
     */
    class PeekAtOtherCard(val player: Int, val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCards[player][index] = cardRevealed

            return if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                TURN_END
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            // Nothing happens, we already know the card

            return if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                TURN_END
        }
    }

    /**
     *
     */
    class BlackKingPeekAsOther(val player: Int, val index: Int) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            /*
            From our perspective, nothing happens. This may change later
            if I add code that keeps track of who has looked at which cards.
            */
            AFTER_PEEK_BLACK_KING
        override fun applyUniqueEffects(game: Game.Determinized) =
            AFTER_PEEK_BLACK_KING
    }

    /**
     *
     */
    class BlackKingSwap(val index: Int) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            (game.actionHistory.last() as BlackKingPeekAsOther).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
        override fun applyUniqueEffects(game: Game.Determinized) =
            (game.actionHistory.last() as BlackKingPeekAsOther).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
    }

    /**
     *
     */
    class Stick(val player: Int, val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            // TODO this card might not be what the user claims it is
            game.playerCards[player].removeAt(index)

            game.discardPile.add(cardRevealed)

            TODO()
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val stuckCard = game.playerCards[player].removeAt(index)

            // Wrong stick
            if (stuckCard != game.discardPile.last()) {
                // Re-insert card
                game.playerCards[player].add(index, stuckCard)

                // TODO Have stickPlayer draw a card
            }

            TODO()
        }
    }

    /**
     *
     */
    class StickAndGiveAway(val stickPlayer: Int, val player: Int, val index: Int, val giveAwayIndex: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            // TODO this card might not be what the user claims it is
            game.playerCards[player].removeAt(index)

            game.discardPile.add(cardRevealed)

            TODO()
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val stuckCard = game.playerCards[player].removeAt(index)

            // Wrong stick
            if (stuckCard != game.discardPile.last()) {
                // Re-insert card
                game.playerCards[player].add(index, stuckCard)

                // TODO Have stickPlayer draw a card
            }

            TODO()
        }
    }

    /**
     *
     */
    object Cambio : NonCardRevealing {
        private fun applyUniqueEffects(game: Game): State {
            game.cambioCaller = game.turn
            game.incTurn()
            return TURN_BEGIN
        }
        override fun applyUniqueEffects(game: Game.PartialInfo) = applyUniqueEffects(game as Game)
        override fun applyUniqueEffects(game: Game.Determinized) = applyUniqueEffects(game as Game)
    }

    /**
     *
     */
    object EndTurn : NonCardRevealing {
        private fun applyUniqueEffects(game: Game): State {
            game.stuck = false
            game.incTurn()
            return if (game.turn == game.cambioCaller)
                GAME_END
            else
                TURN_BEGIN
        }

        override fun applyUniqueEffects(game: Game.PartialInfo) = applyUniqueEffects(game as Game)
        override fun applyUniqueEffects(game: Game.Determinized) = applyUniqueEffects(game as Game)
    }

    object SkipAction : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            TURN_END
        override fun applyUniqueEffects(game: Game.Determinized) =
            TURN_END
    }
}