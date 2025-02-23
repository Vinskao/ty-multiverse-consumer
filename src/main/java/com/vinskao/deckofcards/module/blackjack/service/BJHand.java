package com.vinskao.deckofcards.module.blackjack.service;
import java.util.ArrayList;
import java.util.List;

public class BJHand {
    private List<BJCard> cards;
    public BJHand(){
        this.cards = new ArrayList<>();
    }

    public void addCard(BJCard card){
        cards.add(card);
    }

    public int getHandVal(){
        int value = 0;
        int aceCount = 0;
        for(BJCard card:cards){
            value += card.getValue();
            if("Ace".equals(card.getRank())){
                aceCount++;
            }
        }

        while(value > 21 && aceCount > 0){
            value -= 10;
            aceCount--;
        }

        return value;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && getHandVal() == 21;
    }

    public boolean isBust(){
        return getHandVal() > 21;
    }
    
    @Override
    public String toString(){
        return cards.toString() + " (Value: " + getHandVal() + ")";
    }

    // 返回第一張牌
    public Object getFirstCard() {
        if(cards.isEmpty()){
            return null;
        }
        return cards.get(0);
    }

    // 移除最後一張牌
    public BJCard removeLastCard() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    // 分牌：玩家可以選擇將一張牌分為兩張牌，並且可以選擇其中一張牌進行下注
    public boolean canSplit() {
        if (cards.size() != 2) {
            return false;
        }
        BJCard firstCard = cards.get(0);
        BJCard secondCard = cards.get(1);
        return firstCard.getValue() == secondCard.getValue();
    }
}
