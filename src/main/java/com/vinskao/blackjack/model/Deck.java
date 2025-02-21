package com.vinskao.blackjack.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    protected List<Card> cards;
    protected static final String[] SUITS = {"Hearts", "Diamonds", "Clubs", "Spades"};
    protected static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};

    public Deck(){
        this.cards = new ArrayList<>();
        initDeck();
    }

    public void initDeck(){
        cards.clear();
        for(String suit: SUITS){
            for(String rank: RANKS){
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle(){
        Collections.shuffle(cards);
    }

    public Card drawCard(){
        return cards.isEmpty() ? null: cards.remove(cards.size()-1);
    }

    public void reset(){
        initDeck();
    }

    public int remainingCards(){
        return cards.size();
    }
}
