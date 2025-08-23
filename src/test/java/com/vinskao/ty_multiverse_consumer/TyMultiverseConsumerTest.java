package com.vinskao.ty_multiverse_consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.Card;
import com.vinskao.ty_multiverse_consumer.module.blackjack.service.Deck;

import static org.junit.jupiter.api.Assertions.*;

class TyMultiverseConsumerTest {
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