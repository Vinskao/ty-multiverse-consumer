package com.vinskao.deckofcards.module.blackjack.service;
import com.vinskao.deckofcards.domain.Card;

public class BJCard extends Card {
    public BJCard(String suit, String rank){
        super(suit, rank);
    }

    public int getValue() {
        switch (getRank().toUpperCase()) {
            case "JACK", "QUEEN", "KING" -> {
                return 10;
            }
            case "ACE" -> {
                return 11;
            }
            default -> {
                return Integer.parseInt(getRank());
            }
        }
    }
}
