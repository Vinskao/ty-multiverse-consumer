package com.vinskao.ty_multiverse_consumer.module;

import org.junit.jupiter.api.Test;

import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJCard;

import static org.junit.jupiter.api.Assertions.*;

class BJCardTest {
    @Test
    void testFaceCardValues() {
        BJCard jack = new BJCard("Hearts", "Jack");
        BJCard queen = new BJCard("Diamonds", "Queen");
        BJCard king = new BJCard("Clubs", "King");
        
        assertEquals(10, jack.getValue());
        assertEquals(10, queen.getValue());
        assertEquals(10, king.getValue());
    }

    @Test
    void testAceValue() {
        BJCard ace = new BJCard("Spades", "Ace");
        assertEquals(11, ace.getValue());
    }

    @Test
    void testNumericCardValues() {
        BJCard two = new BJCard("Hearts", "2");
        BJCard ten = new BJCard("Diamonds", "10");
        
        assertEquals(2, two.getValue());
        assertEquals(10, ten.getValue());
    }
} 