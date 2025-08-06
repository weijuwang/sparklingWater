import State.*

/**
 * TODO illegal actions
 * TODO keep track of who looked at which cards
 * TODO how to handle `cardRevealed` discrepancy with stored information
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
     * An [Action] that has the same effects on [Game.PartialInfo] and [Game.Determinized].
     */
    interface SameEffects : NonCardRevealing {
        fun applyUniqueEffects(game: Game): State
        override fun applyUniqueEffects(game: Game.Determinized) = applyUniqueEffects(game as Game)
        override fun applyUniqueEffects(game: Game.PartialInfo) = applyUniqueEffects(game as Game)
    }

    interface TakesPlayerAndIndex : Action {
        val player: Int
        val index: Int
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
     * Draw a card. This should only be used when the current turn is 0.
     */
    object DrawAs0 : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            --game.drawPileSize
            game.drawnCard = cardRevealed
            return AFTER_DRAW
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.drawnCard = game.draw()
            return AFTER_DRAW
        }

        override fun toString() =
            "Draw"
    }

    /**
     * Draw a card. This should only be used when the current turn is not 0.
     */
    object DrawNotAs0 : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            --game.drawPileSize
            game.drawnCard = Card.Unknown()
            return AFTER_DRAW
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.drawnCard = game.draw()
            return AFTER_DRAW
        }

        override fun toString() =
            "Draw"
    }

    /**
     * Discard the card that was just drawn. This should only be used when the current turn is 0.
     */
    object DiscardAs0 : SameEffects {
        override fun applyUniqueEffects(game: Game): State {
            val drawnCard = game.drawnCard as Card.Known
            game.discardPile.add(drawnCard)
            return drawnCard.nextStateWhenDiscarded
        }
        override fun toString() =
            "Discard"
    }

    /**
     * Discard the card that was just drawn. This should only be used when the current turn is not 0.
     */
    object DiscardNotAs0 : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.drawnCard = cardRevealed
            game.discardPile.add(cardRevealed)
            return cardRevealed.nextStateWhenDiscarded
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            DiscardAs0.applyUniqueEffects(game)
        override fun toString() =
            "Discard"
    }

    /**
     * Swap the card that was just drawn for one of the cards the player who drew the card has.
     */
    data class Swap(
        val index: Int
    ) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            // We found out what the card is but it's getting discarded anyways, so no need to update `playerCards`
            game.discardPile.add(cardRevealed)
            // Update the newly drawn card
            game.playerCardInfo[game.turn][index] = game.drawnCard
            return END_OF_TURN
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.discardPile.add(game.playerCards[game.turn][index])
            game.playerCards[game.turn][index] = game.drawnCard as Card.Known
            return END_OF_TURN
        }
        override fun toString() =
            "Swap #$index"
    }

    /**
     * Switch any two cards.
     */
    data class BlindSwitch(
        val playerA: Int,
        val indexA: Int,
        val playerB: Int,
        val indexB: Int
    ) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            val cardA = game.playerCardInfo[playerA][indexA]
            game.playerCardInfo[playerA][indexA] = game.playerCardInfo[playerB][indexB]
            game.playerCardInfo[playerB][indexB] = cardA
            return END_OF_TURN
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val cardA = game.playerCards[playerA][indexA]
            game.playerCards[playerA][indexA] = game.playerCards[playerB][indexB]
            game.playerCards[playerB][indexB] = cardA
            return END_OF_TURN
        }
        override fun toString() =
            "Switch P$playerA's #$indexA with P$playerB's #$indexB"
    }

    /**
     *
     */
    data class PeekAtOwnCardAs0(
        val index: Int
    ) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[0][index] = cardRevealed
            return END_OF_TURN
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            END_OF_TURN
        override fun toString() =
            "Peek #$index"
    }

    /**
     *
     */
    data class PeekAtOwnCardNotAs0(
        val index: Int
    ) : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            END_OF_TURN
        override fun toString() =
            "Peek #$index"
    }

    /**
     *
     */
    data class PeekAtOtherCardAs0(
        override val player: Int,
        override val index: Int
    ) : CardRevealing, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[player][index] = cardRevealed

            return if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                END_OF_TURN
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                END_OF_TURN

        override fun toString() =
            "Peek P$player #$index"
    }

    /**
     *
     */
    data class PeekAtOtherCardNotAs0(
        override val player: Int,
        override val index: Int
    ) : SameEffects, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game) =
            if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                END_OF_TURN
        override fun toString() =
            "Peek P$player #$index"
    }

    /**
     *
     */
    data class BlackKingSwitch(
        val index: Int
    ) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.Determinized) =
            (game.actionHistory.last() as TakesPlayerAndIndex).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            (game.actionHistory.last() as TakesPlayerAndIndex).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
        override fun toString() =
            "Switch with #$index"
    }

    /**
     * Stick a player's own card.
     * This assumes that the stick is valid; otherwise, use [FalseStick].
     * Use [TrueStickAndGiveAway] for sticking another player's card.
     *
     * This is considered [NonCardRevealing] because, given that the stick is valid, we can always deduce what the card must be.
     */
    data class TrueStick(
        override val player: Int,
        override val index: Int
    ) : NonCardRevealing, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            // Remove the stuck card
            game.playerCardInfo[player].removeAt(index)
            // Duplicate the top card on the discard pile, which is equivalent to sticking it.
            // We have to do this because the stuck card might have previously been `Card.Unknown`.
            game.discardPile.add(game.discardPile.last())

            game.stuck = true
            return game.state
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.discardPile.add(
                game.playerCards[player].removeAt(index)
            )

            game.stuck = true
            return game.state
        }
        override fun toString() =
            "P$player sticks #$index"
    }

    /**
     * Stick a card.
     * If the player is sticking their own card, use [TrueStick].
     * If the stick is invalid, use [FalseStick].
     *
     * This is considered [NonCardRevealing] because, given that the stick is valid, we can always deduce what the card must be.
     */
    data class TrueStickAndGiveAway(
        override val player: Int,
        override val index: Int,
        val stickPlayer: Int,
        val giveAwayIndex: Int
    ) : NonCardRevealing, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game.PartialInfo): State {
            // Remove the stuck card
            game.playerCardInfo[player].removeAt(index)
            // Duplicate the top card on the discard pile, which is equivalent to sticking it.
            // We have to do this because the stuck card might have previously been `Card.Unknown`.
            game.discardPile.add(game.discardPile.last())

            // Move the giveaway card
            game.playerCardInfo[player].add(
                game.playerCardInfo[stickPlayer].removeAt(giveAwayIndex)
            )

            // Record that a card was stuck
            game.stuck = true

            return game.state
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            // Stick the card
            game.discardPile.add(
                game.playerCards[player].removeAt(index)
            )

            // Move the giveaway card
            game.playerCards[player].add(
                game.playerCards[stickPlayer].removeAt(giveAwayIndex)
            )

            // Record that a card was stuck
            game.stuck = true

            return game.state
        }
        override fun toString() =
            "P$stickPlayer sticks P$player #$index; gives away #$giveAwayIndex"
    }

    /**
     * An invalid stick from player 0.
     * See [FalseStickNotAs0].
     * Use [TrueStick] and [TrueStickAndGiveAway] for valid sticks.
     */
    data class FalseStickAs0(
        override val player: Int,
        override val index: Int
    ) : CardRevealing, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[0].add(cardRevealed)
            return game.state
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            FalseStickNotAs0(player, index, 0).applyUniqueEffects(game)
        override fun toString() =
            FalseStickNotAs0(player, index, 0).toString()
    }

    /**
     * An invalid stick not from player 0.
     * See [FalseStickAs0].
     * Use [TrueStick] and [TrueStickAndGiveAway] for valid sticks.
     *
     * TODO What can we do with this information? Right now this would tell us what the card isn't but we can't store that
     */
    data class FalseStickNotAs0(
        override val player: Int,
        override val index: Int,
        val stickPlayer: Int
    ) : NonCardRevealing, TakesPlayerAndIndex {
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            game.state
        override fun applyUniqueEffects(game: Game.Determinized): State {
            game.playerCards[stickPlayer].add(game.draw())
            return game.state
        }
        override fun toString() =
            "P0 false-sticks P$player #$index; draws card"
    }

    /**
     * Call "Cambio".
     */
    object Cambio : SameEffects {
        override fun applyUniqueEffects(game: Game): State {
            game.cambioCaller = game.turn
            game.incTurn()
            return BEGINNING_OF_TURN
        }
        override fun toString() =
            "Cambio; end turn"
    }

    /**
     * End the turn. This bars any players from sticking.
     */
    object EndTurn : SameEffects {
        override fun applyUniqueEffects(game: Game): State {
            game.stuck = false
            game.incTurn()
            return if (game.turn == game.cambioCaller)
                END_OF_GAME
            else
                BEGINNING_OF_TURN
        }
        override fun toString() =
            "End turn"
    }

    /**
     * Skip an optional action resulting from discarding a card.
     */
    object SkipAction : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            END_OF_TURN
        override fun toString() =
            "N/A"
    }
}