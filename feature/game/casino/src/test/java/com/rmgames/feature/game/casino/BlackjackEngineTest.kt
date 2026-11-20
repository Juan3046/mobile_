package com.rmgames.feature.game.casino

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackjackEngineTest {
    @Test fun `initial deal exposes one card per step`() {
        val engine = engine(
            listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS)),
            listOf(Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.SEVEN, Suit.DIAMONDS)),
        )
        engine.start()
        assertEquals(CasinoPhase.DEALING, engine.state.value.phase)
        repeat(3) {
            engine.dealNextInitialCard()
            assertEquals(CasinoPhase.DEALING, engine.state.value.phase)
        }
        engine.dealNextInitialCard()
        engine.finishInitialDeal()
        assertEquals(CasinoPhase.PLAYING, engine.state.value.phase)
        assertEquals(2, engine.state.value.player.cards.size)
        assertEquals(2, engine.state.value.dealer.cards.size)
    }

    @Test fun `aces count as eleven when possible and one when needed`() {
        assertEquals(21, hand(Card(Rank.ACE, Suit.CLUBS), Card(Rank.KING, Suit.CLUBS)).total)
        assertEquals(16, hand(Card(Rank.ACE, Suit.CLUBS), Card(Rank.KING, Suit.CLUBS), Card(Rank.FIVE, Suit.CLUBS)).total)
        assertTrue(hand(Card(Rank.ACE, Suit.CLUBS), Card(Rank.ACE, Suit.DIAMONDS), Card(Rank.FIVE, Suit.HEARTS)).isSoft)
    }

    @Test fun `dealer stands on soft seventeen`() {
        val engine = engine(
            listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.CLUBS)),
            listOf(Card(Rank.ACE, Suit.DIAMONDS), Card(Rank.SIX, Suit.DIAMONDS)),
        )
        startHand(engine)
        engine.stand()
        assertEquals(CasinoPhase.DEALER_TURN, engine.state.value.phase)
        engine.dealerStep()
        assertEquals(HandResult.PUSH, engine.state.value.lastResult)
        assertEquals(100, engine.state.value.balance)
    }

    @Test fun `ordinary win pays ten net and push returns no change`() {
        val win = engine(
            listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS)),
            listOf(Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.SEVEN, Suit.DIAMONDS)),
        )
        startHand(win); win.stand(); win.dealerStep()
        assertEquals(110, win.state.value.balance)

        val push = engine(
            listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.CLUBS)),
            listOf(Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.NINE, Suit.DIAMONDS)),
        )
        startHand(push); push.stand(); push.dealerStep()
        assertEquals(100, push.state.value.balance)
    }

    @Test fun `natural blackjack pays fifteen net`() {
        val engine = engine(
            listOf(Card(Rank.ACE, Suit.CLUBS), Card(Rank.KING, Suit.CLUBS)),
            listOf(Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.SEVEN, Suit.DIAMONDS)),
        )
        startHand(engine)
        assertEquals(115, engine.state.value.balance)
        assertEquals(HandResult.BLACKJACK, engine.state.value.lastResult)
    }

    @Test fun `matching dealer and player naturals push`() {
        val engine = engine(
            listOf(Card(Rank.ACE, Suit.CLUBS), Card(Rank.KING, Suit.CLUBS)),
            listOf(Card(Rank.ACE, Suit.DIAMONDS), Card(Rank.QUEEN, Suit.DIAMONDS)),
        )
        startHand(engine)
        assertEquals(100, engine.state.value.balance)
        assertEquals(HandResult.PUSH, engine.state.value.lastResult)
    }

    @Test fun `bust loses ten and ends when balance is below minimum wager`() {
        val deck = listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.SEVEN, Suit.DIAMONDS), Card(Rank.FIVE, Suit.HEARTS))
        val engine = BlackjackEngine(initialBalance = 10, deckFactory = { deck.toMutableList() })
        startHand(engine)
        engine.hit()
        assertEquals(0, engine.state.value.balance)
        assertTrue(engine.state.value.phase == CasinoPhase.FINISHED)
    }

    @Test fun `player decision remains open until an explicit action`() {
        val engine = BlackjackEngine(deckFactory = {
            listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS), Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS)).toMutableList()
        })
        startHand(engine)
        assertEquals(CasinoPhase.PLAYING, engine.state.value.phase)
        assertEquals("Your turn: nod to hit or shake to stand", engine.state.value.message)
        assertTrue(engine.state.value.lastResult == null)
    }

    @Test fun `reset restores starting balance for a replay`() {
        val engine = BlackjackEngine(initialBalance = 35)
        engine.start()
        engine.reset()
        assertEquals(35, engine.state.value.balance)
        assertEquals(CasinoPhase.WAITING, engine.state.value.phase)
        assertTrue(engine.state.value.player.cards.isEmpty())
    }

    private fun engine(player: List<Card>, dealer: List<Card>): BlackjackEngine {
        val orderedDeal = listOf(player[0], dealer[0], player[1], dealer[1])
        val deck = (orderedDeal + listOf(Card(Rank.TWO, Suit.HEARTS), Card(Rank.THREE, Suit.SPADES)))
        return BlackjackEngine(deckFactory = { deck.toMutableList() })
    }

    private fun startHand(engine: BlackjackEngine) {
        engine.start()
        repeat(4) { engine.dealNextInitialCard() }
        engine.finishInitialDeal()
    }

    private fun hand(vararg cards: Card) = BlackjackHand(cards.toList())
}
