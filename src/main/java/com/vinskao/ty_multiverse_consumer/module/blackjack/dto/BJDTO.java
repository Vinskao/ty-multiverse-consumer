package com.vinskao.ty_multiverse_consumer.module.blackjack.dto;

import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.GameState;
import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.GameState.State;
import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJCard;
import java.util.List;
import java.util.ArrayList;

public class BJDTO {
    private List<CardDTO> playerCards;
    private List<CardDTO> dealerCards;
    private int playerScore;
    private Integer dealerScore;
    private State gameState;
    private boolean canHit;
    private boolean canStand;
    private boolean canDouble;
    private boolean canSplit;
    private String message;
    
    // 內部類用於轉換卡牌資訊
    public static class CardDTO {
        private String suit;
        private String rank;
        private String display;
        
        public CardDTO(BJCard card) {
            this.suit = card.getSuit();
            this.rank = card.getRank();
            this.display = card.getRank() + " of " + card.getSuit();
        }
        
        // Getters
        public String getSuit() { return suit; }
        public String getRank() { return rank; }
        public String getDisplay() { return display; }
    }
    
    // Constructor
    public BJDTO(GameState gameState) {
        this.playerCards = convertToCardDTOs(gameState.getPlayerHand().getCards());
        this.dealerCards = convertToCardDTOs(gameState.getDealerHand().getCards());
        this.playerScore = gameState.getPlayerScore();
        this.dealerScore = gameState.getDealerScore();
        this.gameState = gameState.getState();
        
        // 設置可用操作
        updateAvailableActions(gameState);
    }
    
    private List<CardDTO> convertToCardDTOs(List<BJCard> cards) {
        List<CardDTO> cardDTOs = new ArrayList<>();
        for (BJCard card : cards) {
            cardDTOs.add(new CardDTO(card));
        }
        return cardDTOs;
    }
    
    private void updateAvailableActions(GameState gameState) {
        State currentState = gameState.getState();
        this.canHit = currentState == State.PLAYER_TURN;
        this.canStand = currentState == State.PLAYER_TURN;
        this.canDouble = currentState == State.PLAYER_TURN && gameState.getPlayerHand().getCards().size() == 2;
        this.canSplit = currentState == State.PLAYER_TURN && gameState.getPlayerHand().canSplit();
    }
    
    // Getters
    public List<CardDTO> getPlayerCards() { return playerCards; }
    public List<CardDTO> getDealerCards() { return dealerCards; }
    public int getPlayerScore() { return playerScore; }
    public Integer getDealerScore() { return dealerScore; }
    public State getGameState() { return gameState; }
    public boolean isCanHit() { return canHit; }
    public boolean isCanStand() { return canStand; }
    public boolean isCanDouble() { return canDouble; }
    public boolean isCanSplit() { return canSplit; }
    public String getMessage() { return message; }
    
    // Setter for message
    public void setMessage(String message) { this.message = message; }
} 