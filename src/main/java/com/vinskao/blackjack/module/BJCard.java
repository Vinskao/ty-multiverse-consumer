package com.vinskao.blackjack.module;
import com.vinskao.blackjack.model.Card;

public class BJCard extends Card {
    public BJCard(String suit, String rank){
        super(suit, rank);
    }

    public int getValue(){
        return switch (getRank()){
            case "Jack","Queen","King" -> 10;
            case "Ace" -> 11;
            default -> Integer.parseInt(getRank());
        };
    }
}
