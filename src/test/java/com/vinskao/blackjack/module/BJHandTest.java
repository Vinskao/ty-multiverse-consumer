package com.vinskao.blackjack.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BJHandTest {
    private BJHand hand;

    @BeforeEach
    void setUp() {
        hand = new BJHand();
    }

    @Test
    void testBlackjack() {
        hand.addCard(new BJCard("Hearts", "Ace"));
        hand.addCard(new BJCard("Spades", "King"));
        assertTrue(hand.isBlackJack());
    }

    @Test
    void testBust() {
        hand.addCard(new BJCard("Hearts", "King"));
        hand.addCard(new BJCard("Diamonds", "Queen"));
        hand.addCard(new BJCard("Clubs", "Jack"));
        assertTrue(hand.isBust());
    }

    @Test
    void testAceValueAdjustment() {
        hand.addCard(new BJCard("Hearts", "Ace"));
        hand.addCard(new BJCard("Diamonds", "Ace"));
        assertEquals(12, hand.getHandVal());
    }
} 