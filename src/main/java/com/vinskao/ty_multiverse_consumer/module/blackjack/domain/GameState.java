package com.vinskao.ty_multiverse_consumer.module.blackjack.domain;

import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJHand;

public class GameState {
    private final BJHand playerHand;
    private final BJHand dealerCards;
    private final State state;
    private final int playerScore;
    private final Integer dealerScore;

    public GameState(BJHand playerHand, BJHand dealerCards, State currentState, 
                    int playerScore, Integer dealerScore) {
        this.playerHand = playerHand;
        this.dealerCards = dealerCards;
        this.state = currentState;
        this.playerScore = playerScore;
        this.dealerScore = dealerScore;
    }

    // Add an enum for the actual states
    public enum State {
        GAME_START,
        BETTING,
        DEAL_INITIAL_CARDS,
        CHECK_BLACKJACK,
        PLAYER_TURN,
        PLAYER_HIT,
        PLAYER_STAND,
        PLAYER_DOUBLE,
        PLAYER_SPLIT,
        PLAYER_BUST,
        DEALER_TURN,
        DEALER_HIT,
        DEALER_STAND,
        DEALER_BUST,
        GAME_RESULT,
        PAYOUT,
        GAME_END,
        PLAYER_WIN,
        DEALER_WIN,
        PUSH
    }

    // Getters
    public BJHand getPlayerHand() {
        return playerHand;
    }

    public BJHand getDealerCards() {
        return dealerCards;
    }

    public State getState() {
        return state;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public Integer getDealerScore() {
        return dealerScore;
    }

    public BJHand getDealerHand() {
        return dealerCards;
    }
}
