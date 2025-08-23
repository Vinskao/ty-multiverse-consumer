package com.vinskao.ty_multiverse_consumer.model;

import org.junit.jupiter.api.Test;

import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.Card;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {
    @Test
    void testCardCreation() {
        Card card = new Card("Hearts", "Ace");
        assertEquals("Hearts", card.getSuit());
        assertEquals("Ace", card.getRank());
    }

    @Test
    void testToString() {
        Card card = new Card("Spades", "King");
        assertEquals("KingofSpades", card.toString());
    }
} 