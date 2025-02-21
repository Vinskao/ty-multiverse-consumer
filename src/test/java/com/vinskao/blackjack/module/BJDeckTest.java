package com.vinskao.blackjack.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BJDeckTest {
    private BJDeck deck;

    @BeforeEach
    void setUp() {
        deck = new BJDeck();
    }

    @Test
    void testDrawBJCard() {
        assertTrue(deck.drawCard() instanceof BJCard);
    }

    @Test
    void testInitialDeckSize() {
        assertEquals(52, deck.remainingCards());
    }
} 