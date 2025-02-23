package com.vinskao.deckofcards.module.blackjack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vinskao.deckofcards.domain.GameState;
import com.vinskao.deckofcards.domain.GameState.State;

@Service
public class BJService {
    private BJDeck deck;
    private BJHand playerHand;
    private BJHand dealerHand;
    private State currentState;

    @Autowired
    private GameStateManager stateManager;

    public BJService() {
        this.deck = new BJDeck();
        this.currentState = State.GAME_END; // Initialize with GAME_END state
    }

    // 開始新遊戲
    public GameState startNewGame() {
        // If we're in the middle of a game, we need to properly end it first
        if (currentState != State.GAME_END) {
            if (currentState == State.PLAYER_TURN) {
                // Force a stand to end the current game
                validateAndUpdateState(State.PLAYER_STAND);
                validateAndUpdateState(State.DEALER_TURN);
                validateAndUpdateState(State.GAME_RESULT);
                validateAndUpdateState(State.GAME_END);
            } else {
                validateAndUpdateState(State.GAME_END);
            }
        }
        
        // Now we can safely start a new game
        validateAndUpdateState(State.GAME_START);
        validateAndUpdateState(State.BETTING);
        validateAndUpdateState(State.DEAL_INITIAL_CARDS);
        
        deck.reset();
        playerHand = new BJHand();
        dealerHand = new BJHand();
        
        // Deal initial cards
        playerHand.addCard((BJCard) deck.drawCard());
        dealerHand.addCard((BJCard) deck.drawCard());
        playerHand.addCard((BJCard) deck.drawCard());
        dealerHand.addCard((BJCard) deck.drawCard());
        
        validateAndUpdateState(State.CHECK_BLACKJACK);
        
        // Check for blackjack
        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            validateAndUpdateState(State.GAME_RESULT);
        } else {
            validateAndUpdateState(State.PLAYER_TURN);
        }
        
        return createGameState();
    }

    // 玩家抽牌
    public GameState playerHit() {
        validateAndUpdateState(State.PLAYER_HIT);
        
        playerHand.addCard((BJCard) deck.drawCard());
        
        if (playerHand.isBust()) {
            validateAndUpdateState(State.PLAYER_BUST);
        } else {
            validateAndUpdateState(State.PLAYER_TURN);
        }
        
        return createGameState();
    }

    // 玩家停牌
    public GameState playerStand() {
        validateAndUpdateState(State.PLAYER_STAND);
        validateAndUpdateState(State.DEALER_TURN);

        while (dealerHand.getHandVal() < 17) {
            dealerHand.addCard((BJCard) deck.drawCard());
        }

        // Add proper dealer state transition
        if (dealerHand.isBust()) {
            validateAndUpdateState(State.DEALER_BUST);
        } else {
            validateAndUpdateState(State.DEALER_STAND);
        }

        State result = determineWinner();
        validateAndUpdateState(State.GAME_RESULT);
        validateAndUpdateState(result);
        validateAndUpdateState(State.GAME_END);
        return createGameState();
    }

    // 確定勝者
    private State determineWinner() {
        if (playerHand.isBust()) {
            return State.DEALER_WIN;
        }
        if (dealerHand.isBust()) {
            return State.PLAYER_WIN;
        }
        
        int playerValue = playerHand.getHandVal();
        int dealerValue = dealerHand.getHandVal();
        
        if (playerValue > dealerValue) {
            return State.PLAYER_WIN;
        } else if (playerValue < dealerValue) {
            return State.DEALER_WIN;
        } else {
            return State.PUSH;
        }
    }

    // 获取游戏状态
    public Object getGameStatus() {
        if (playerHand == null || dealerHand == null) {
            throw new IllegalStateException("No game has been started");
        }
        return createGameState();
    }

    // 创建游戏状态
    private GameState createGameState() {
        return new GameState(
            playerHand,
            dealerHand,
            currentState,
            playerHand.getHandVal(),
            dealerHand.getHandVal()
        );
    }

    private void validateAndUpdateState(State nextState) {
        stateManager.validateStateTransition(currentState, nextState);
        currentState = nextState;
    }

    public GameState playerDouble() {
        validateAndUpdateState(State.PLAYER_DOUBLE);
        
        // Draw one card and end player's turn
        playerHand.addCard((BJCard) deck.drawCard());
        
        validateAndUpdateState(State.DEALER_TURN);
        
        // Dealer plays their hand
        while (dealerHand.getHandVal() < 17) {
            dealerHand.addCard((BJCard) deck.drawCard());
        }
        
        // Add proper dealer state transition
        if (dealerHand.isBust()) {
            validateAndUpdateState(State.DEALER_BUST);
        } else {
            validateAndUpdateState(State.DEALER_STAND);
        }
        
        State result = determineWinner();
        validateAndUpdateState(State.GAME_RESULT);
        validateAndUpdateState(result);
        validateAndUpdateState(State.GAME_END);
        return createGameState();
    }

    public GameState playerSplit() {
        validateAndUpdateState(State.PLAYER_SPLIT);
        
        if (!playerHand.canSplit()) {
            throw new IllegalStateException("Cannot split current hand");
        }
        
        // Create new hand for split
        BJCard splitCard = playerHand.removeLastCard();
        BJHand splitHand = new BJHand();
        splitHand.addCard(splitCard);
        
        // Draw one card for each hand
        playerHand.addCard((BJCard) deck.drawCard());
        splitHand.addCard((BJCard) deck.drawCard());
        
        // TODO: Handle split hand gameplay
        // For now, just continue with original hand
        validateAndUpdateState(State.PLAYER_TURN);
        return createGameState();
    }
} 