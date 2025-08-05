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
            game.drawnCard = game.drawRandom()
            return AFTER_DRAW
        }
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
            game.drawnCard = game.drawRandom()
            return AFTER_DRAW
        }
    }

    /**
     * Discard the card that was just drawn. This should only be used when the current turn is 0.
     */
    object DiscardAs0 : NonCardRevealing, SameEffects {
        override fun applyUniqueEffects(game: Game): State {
            val drawnCard = game.drawnCard as Card.Known
            game.discardPile.add(drawnCard)
            return drawnCard.nextStateWhenDiscarded
        }
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
    }

    /**
     * Swap the card that was just drawn for one of the cards the player who drew the card has.
     */
    class Swap(val index: Int) : CardRevealing {
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
    }

    /**
     * Switch any two cards.
     */
    class BlindSwitch(val playerA: Int, val indexA: Int, val playerB: Int, val indexB: Int) : NonCardRevealing {
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
    }

    /**
     *
     */
    class PeekAtOwnCardAs0(val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[0][index] = cardRevealed
            return END_OF_TURN
        }
        override fun applyUniqueEffects(game: Game.Determinized) =
            END_OF_TURN
    }

    /**
     *
     */
    class PeekAtOwnCardNotAs0(val index: Int) : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            END_OF_TURN
    }

    /**
     *
     */
    class PeekAtOtherCardAs0(val player: Int, val index: Int) : CardRevealing {
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
    }

    /**
     *
     */
    class PeekAtOtherCardNotAs0(val player: Int, val index: Int) : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            if(game.state == AFTER_DISCARD_BLACK_KING)
                AFTER_PEEK_BLACK_KING
            else
                END_OF_TURN
    }

    /**
     *
     */
    class BlackKingPeekAsOther(val player: Int, val index: Int) : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            AFTER_PEEK_BLACK_KING
    }

    /**
     *
     */
    class BlackKingSwap(val index: Int) : NonCardRevealing {
        override fun applyUniqueEffects(game: Game.Determinized) =
            (game.actionHistory.last() as BlackKingPeekAsOther).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
        override fun applyUniqueEffects(game: Game.PartialInfo) =
            (game.actionHistory.last() as BlackKingPeekAsOther).let {
                BlindSwitch(it.player, it.index, game.turn, index)
                    .applyUniqueEffects(game)
            }
    }

    /**
     * Stick a card. If the player is not sticking their own card, use [StickAndGiveAway].
     */
    class Stick(val player: Int, val index: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[player].removeAt(index)

            game.discardPile.add(cardRevealed)

            TODO("Stick")
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val stuckCard = game.playerCards[player].removeAt(index)

            // Wrong stick
            if (stuckCard != game.discardPile.last()) {
                // Re-insert card
                game.playerCards[player].add(index, stuckCard)

                // TODO Have stickPlayer draw a card
            }

            TODO("Stick")
        }
    }

    /**
     * Stick a card. If the player is sticking their own card, use [Stick].
     */
    class StickAndGiveAway(val stickPlayer: Int, val player: Int, val index: Int, val giveAwayIndex: Int) : CardRevealing {
        override fun applyUniqueEffects(game: Game.PartialInfo, cardRevealed: Card.Known): State {
            game.playerCardInfo[player].removeAt(index)

            game.discardPile.add(cardRevealed)

            TODO("StickAndGiveAway")
        }
        override fun applyUniqueEffects(game: Game.Determinized): State {
            val stuckCard = game.playerCards[player].removeAt(index)

            // Wrong stick
            if (stuckCard != game.discardPile.last()) {
                // Re-insert card
                game.playerCards[player].add(index, stuckCard)

                // TODO Have stickPlayer draw a card
            }

            TODO("StickAndGiveAway")
        }
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
    }

    /**
     * Skip an optional action resulting from discarding a card.
     */
    object SkipAction : SameEffects {
        override fun applyUniqueEffects(game: Game) =
            END_OF_TURN
    }
}