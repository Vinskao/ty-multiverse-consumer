package com.vinskao.ty_multiverse_consumer.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJCard;
import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJHand;

import static org.junit.jupiter.api.Assertions.*;

class BJHandTest {
    private BJHand hand;

    @BeforeEach
    void setUp() {
        hand = new BJHand();
    }

    @Test
    void testty_multiverse_consumer() {
        hand.addCard(new BJCard("Hearts", "Ace"));
        hand.addCard(new BJCard("Spades", "King"));
        assertTrue(hand.isBlackjack());
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