package com.vinskao.blackjack.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeckTest {
    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Test
    void testInitialDeckSize() {
        assertEquals(52, deck.remainingCards());
    }

    @Test
    void testDrawCard() {
        Card card = deck.drawCard();
        assertNotNull(card);
        assertEquals(51, deck.remainingCards());
    }

    @Test
    void testReset() {
        deck.drawCard();
        deck.reset();
        assertEquals(52, deck.remainingCards());
    }
} 