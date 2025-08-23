package com.vinskao.ty_multiverse_consumer.module.blackjack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.GameState;
import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.GameState.State;

@Service
public class BJService {
    private BJDeck deck;
    private BJHand playerHand;
    private BJHand dealerHand;
    private State currentState;

    @Autowired
    private GameStateManager stateManager;

    private static final long DEALER_TURN_TIMEOUT = 3000; // 3 seconds timeout
    private long dealerTurnStartTime;

    // 初始化遊戲
    public BJService() {
        this.deck = new BJDeck();
        this.currentState = State.GAME_END; // Initialize with GAME_END state
    }

    // 開始新遊戲
    public GameState startNewGame() {
        if (currentState != State.GAME_END) {
            if (currentState == State.PLAYER_TURN) {
                // Force a stand to end the current game
                validateAndUpdateState(State.PLAYER_STAND);
                validateAndUpdateState(State.DEALER_TURN);
            } 

            // 確保遊戲結果狀態的正確轉換
            if (currentState == State.PLAYER_WIN || 
                currentState == State.DEALER_WIN || 
                currentState == State.PUSH) {
                validateAndUpdateState(State.GAME_RESULT);
            }

            if (currentState == State.GAME_RESULT || 
                currentState == State.PLAYER_BUST || 
                currentState == State.DEALER_BUST) {
                validateAndUpdateState(State.GAME_END);
            }
        }
        
        // 開始新遊戲的其餘邏輯保持不變
        validateAndUpdateState(State.GAME_START);
        validateAndUpdateState(State.BETTING);
        validateAndUpdateState(State.DEAL_INITIAL_CARDS);
        
        // 重置牌組和手牌
        deck.reset();
        playerHand = new BJHand();
        dealerHand = new BJHand();
        
        // 發初始牌
        playerHand.addCard((BJCard) deck.drawCard());
        dealerHand.addCard((BJCard) deck.drawCard());
        playerHand.addCard((BJCard) deck.drawCard());
        dealerHand.addCard((BJCard) deck.drawCard());
        
        validateAndUpdateState(State.CHECK_BLACKJACK);
        
        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            validateAndUpdateState(State.GAME_RESULT);
        } else {
            validateAndUpdateState(State.PLAYER_TURN);
        }
        
        return createGameState();
    }

    // 玩家抽牌
    public GameState playerHit() {
        //
        validateAndUpdateState(State.PLAYER_HIT);
        
        // 玩家抽
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
        
        dealerTurnStartTime = System.currentTimeMillis();
        
        try {
            while (dealerHand.getHandVal() < 17) {
                // Check for timeout
                if (System.currentTimeMillis() - dealerTurnStartTime > DEALER_TURN_TIMEOUT) {
                    validateAndUpdateState(State.GAME_RESULT);
                    validateAndUpdateState(State.PUSH);
                    validateAndUpdateState(State.GAME_END);
                    return createGameState();
                }
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
            
        } catch (Exception e) {
            // If any error occurs during dealer's turn, end the game as a push
            validateAndUpdateState(State.GAME_RESULT);
            validateAndUpdateState(State.PUSH);
            validateAndUpdateState(State.GAME_END);
            return createGameState();
        }
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
        
        playerHand.addCard((BJCard) deck.drawCard());
        validateAndUpdateState(State.DEALER_TURN);
        
        dealerTurnStartTime = System.currentTimeMillis();
        
        try {
            while (dealerHand.getHandVal() < 17) {
                if (System.currentTimeMillis() - dealerTurnStartTime > DEALER_TURN_TIMEOUT) {
                    validateAndUpdateState(State.GAME_RESULT);
                    validateAndUpdateState(State.PUSH);
                    validateAndUpdateState(State.GAME_END);
                    return createGameState();
                }
                dealerHand.addCard((BJCard) deck.drawCard());
            }
            
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
            
        } catch (Exception e) {
            validateAndUpdateState(State.GAME_RESULT);
            validateAndUpdateState(State.PUSH);
            validateAndUpdateState(State.GAME_END);
            return createGameState();
        }
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
        
        // 玩家抽牌
        validateAndUpdateState(State.PLAYER_TURN);
        return createGameState();
    }
} 