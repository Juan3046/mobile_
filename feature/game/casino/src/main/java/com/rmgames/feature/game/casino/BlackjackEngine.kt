package com.rmgames.feature.game.casino

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
enum class Rank(val label: String, val points: Int) {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), SIX("6", 6),
    SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
    JACK("J", 10), QUEEN("Q", 10), KING("K", 10), ACE("A", 11),
}

data class Card(val rank: Rank, val suit: Suit) {
    val symbol: String get() = when (suit) {
        Suit.CLUBS -> "♣"
        Suit.DIAMONDS -> "♦"
        Suit.HEARTS -> "♥"
        Suit.SPADES -> "♠"
    }
}

data class BlackjackHand(val cards: List<Card>) {
    val total: Int
        get() {
            var value = cards.sumOf { it.rank.points }
            var aces = cards.count { it.rank == Rank.ACE }
            while (value > BLACKJACK && aces > 0) {
                value -= 10
                aces--
            }
            return value
        }
    val isBust: Boolean get() = total > BLACKJACK
    val isNatural: Boolean get() = cards.size == 2 && total == BLACKJACK
    val isSoft: Boolean
        get() {
            val hardTotal = cards.sumOf { if (it.rank == Rank.ACE) 1 else it.rank.points }
            return cards.any { it.rank == Rank.ACE } && hardTotal + 10 == total
        }

    fun add(card: Card) = copy(cards = cards + card)

    companion object { const val BLACKJACK = 21 }
}

enum class CasinoPhase { WAITING, DEALING, PLAYING, DEALER_TURN, BETWEEN_HANDS, FINISHED }
enum class HandResult { WIN, LOSS, PUSH, BLACKJACK }

data class CasinoState(
    val player: BlackjackHand = BlackjackHand(emptyList()),
    val dealer: BlackjackHand = BlackjackHand(emptyList()),
    val balance: Int = BlackjackEngine.STARTING_BALANCE,
    val phase: CasinoPhase = CasinoPhase.WAITING,
    val lastResult: HandResult? = null,
    val message: String = "Get ready",
    /** Current hand, 1..[BlackjackEngine.HANDS_PER_RUN]; 0 before the first deal. */
    val hand: Int = 0,
) {
    val dealerCardsVisible: Boolean
        get() = phase in setOf(CasinoPhase.DEALER_TURN, CasinoPhase.BETWEEN_HANDS, CasinoPhase.FINISHED)
}

/**
 * Pure blackjack/run rules. A deck factory supplies a fresh shuffled deck per hand.
 * A run is exactly [HANDS_PER_RUN] hands (playtest 2026-11-20: an endless run was a problem);
 * it ends earlier only if the bananas run out.
 */
class BlackjackEngine(
    private val initialBalance: Int = STARTING_BALANCE,
    private val deckFactory: () -> MutableList<Card> = ::shuffledDeck,
) {
    private val _state = MutableStateFlow(CasinoState(balance = initialBalance))
    val state: StateFlow<CasinoState> = _state.asStateFlow()
    private var deck: MutableList<Card> = mutableListOf()
    private var initialDealIndex = 0

    init { require(initialBalance >= 0) }

    fun reset() {
        deck.clear()
        initialDealIndex = 0
        _state.value = CasinoState(balance = initialBalance)
    }

    fun start() {
        if (_state.value.phase != CasinoPhase.WAITING) return
        beginHand()
    }

    /** Adds exactly one of the four initial cards so the UI can pace the deal. */
    fun dealNextInitialCard(): Card? {
        if (_state.value.phase != CasinoPhase.DEALING || initialDealIndex >= INITIAL_CARDS) return null
        val card = draw()
        val current = _state.value
        val player = current.player
        val dealer = current.dealer
        val updated = when (initialDealIndex) {
            0 -> current.copy(player = player.add(card), message = "Your first card: ${card.description()}")
            1 -> current.copy(dealer = dealer.add(card), message = "Dealer's upcard: ${card.description()}")
            2 -> current.copy(player = player.add(card), message = "Your second card: ${card.description()}")
            else -> current.copy(dealer = dealer.add(card), message = "Dealer receives a hidden card")
        }
        initialDealIndex++
        _state.value = updated
        return card
    }

    fun finishInitialDeal() {
        if (_state.value.phase != CasinoPhase.DEALING || initialDealIndex != INITIAL_CARDS) return
        val current = _state.value
        _state.value = current.copy(phase = CasinoPhase.PLAYING, message = "Your turn: nod to hit or shake to stand")
        when {
            current.player.isNatural && current.dealer.isNatural -> settle(HandResult.PUSH)
            current.player.isNatural -> settle(HandResult.BLACKJACK)
            current.dealer.isNatural -> settle(HandResult.LOSS)
        }
    }

    fun hit() {
        val current = _state.value
        if (current.phase != CasinoPhase.PLAYING) return
        val player = current.player.add(draw())
        _state.value = current.copy(
            player = player,
            message = "You drew ${player.cards.last().description()}. Your total is ${player.total}.",
        )
        when {
            player.isBust -> settle(HandResult.LOSS)
            player.total == BlackjackHand.BLACKJACK -> stand()
        }
    }

    fun stand() {
        val current = _state.value
        if (current.phase != CasinoPhase.PLAYING) return
        _state.value = current.copy(
            phase = CasinoPhase.DEALER_TURN,
            message = "You stand on ${current.player.total}. The dealer reveals the hidden card.",
        )
    }

    /** Draws one dealer card, or settles once the dealer must stand. */
    fun dealerStep(): Card? {
        val current = _state.value
        if (current.phase != CasinoPhase.DEALER_TURN) return null
        var dealer = current.dealer
        if (dealer.total < DEALER_STAND_TOTAL) {
            val card = draw()
            dealer = dealer.add(card)
            _state.value = current.copy(
                dealer = dealer,
                message = "Dealer draws ${card.description()}. Dealer total: ${dealer.total}.",
            )
            return card
        }
        val result = when {
            dealer.isBust -> HandResult.WIN
            current.player.total > dealer.total -> HandResult.WIN
            current.player.total < dealer.total -> HandResult.LOSS
            else -> HandResult.PUSH
        }
        settle(result)
        return null
    }

    fun beginNextHand() {
        val current = _state.value
        if (current.phase != CasinoPhase.BETWEEN_HANDS) return
        if (current.balance < WAGER) {
            finishRun("Out of bananas")
        } else if (current.hand >= HANDS_PER_RUN) {
            finishRun("$HANDS_PER_RUN hands played · you finish with ${current.balance} bananas")
        } else {
            beginHand()
        }
    }

    private fun beginHand() {
        if (_state.value.balance < WAGER) {
            finishRun("Out of bananas")
            return
        }
        deck = deckFactory()
        require(deck.size >= 4) { "A blackjack deck must contain at least four cards" }
        initialDealIndex = 0
        val hand = _state.value.hand + 1
        _state.value = _state.value.copy(
            player = BlackjackHand(emptyList()),
            dealer = BlackjackHand(emptyList()),
            phase = CasinoPhase.DEALING,
            lastResult = null,
            hand = hand,
            message = if (hand == HANDS_PER_RUN) "Last hand! Shuffling the deck…" else "Hand $hand of $HANDS_PER_RUN · shuffling the deck…",
        )
    }

    private fun settle(result: HandResult) {
        val current = _state.value
        if (current.phase in setOf(CasinoPhase.WAITING, CasinoPhase.BETWEEN_HANDS, CasinoPhase.FINISHED)) return
        val payout = when (result) {
            HandResult.WIN -> WAGER
            HandResult.BLACKJACK -> WAGER * 3 / 2
            HandResult.LOSS, HandResult.PUSH -> 0
        }
        val balance = current.balance + payout - if (result == HandResult.LOSS) WAGER else 0
        val dealerStatus = if (current.dealer.isBust) {
            "Dealer busts at ${current.dealer.total}"
        } else {
            "Dealer stands on ${current.dealer.total}"
        }
        val message = when (result) {
            HandResult.WIN -> "$dealerStatus. You win +$WAGER bananas"
            HandResult.LOSS -> when {
                current.player.isBust -> "You bust at ${current.player.total}. You lose −$WAGER bananas"
                current.dealer.isNatural -> "Dealer has a natural blackjack. You lose −$WAGER bananas"
                else -> "$dealerStatus. Dealer wins; you lose −$WAGER bananas"
            }
            HandResult.PUSH -> "$dealerStatus. Push · bet returned"
            HandResult.BLACKJACK -> "Natural blackjack! You win +${WAGER * 3 / 2} bananas"
        }
        _state.value = current.copy(
            balance = balance,
            phase = CasinoPhase.BETWEEN_HANDS,
            lastResult = result,
            message = message,
        )
        if (balance < WAGER) finishRun("Out of bananas")
    }

    private fun finishRun(reason: String) {
        _state.value = _state.value.copy(phase = CasinoPhase.FINISHED, message = reason)
    }

    private fun draw(): Card {
        check(deck.isNotEmpty()) { "Blackjack deck is empty" }
        return deck.removeAt(0)
    }

    companion object {
        const val STARTING_BALANCE = 100
        const val WAGER = 10
        const val HANDS_PER_RUN = 5
        private const val INITIAL_CARDS = 4
        private const val DEALER_STAND_TOTAL = 17

        private fun shuffledDeck(): MutableList<Card> = Suit.entries
            .flatMap { suit -> Rank.entries.map { rank -> Card(rank, suit) } }
            .shuffled()
            .toMutableList()
    }
}

private fun Card.description(): String = "${rank.label} of ${suit.name.lowercase()}"
