package com.vinskao.deckofcards.module.blackjack.service;

import com.vinskao.deckofcards.domain.GameState;
import com.vinskao.deckofcards.domain.GameState.State;
import com.vinskao.deckofcards.util.JsonReader;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class GameStateManager {
    private final JsonReader jsonReader;
    private Map<String, Map<String, Object>> stateConfig;
    
    public GameStateManager() {
        this.jsonReader = new JsonReader();
        loadStateConfig();
    }
    
    @SuppressWarnings("unchecked")
    private void loadStateConfig() {
        Map<String, Map<String, Map<String, Object>>> config = 
            jsonReader.readConfig("states.json", Map.class);
        this.stateConfig = config.get("gameStates");
    }
    
    public boolean isValidTransition(State currentState2, GameState.State next) {
        Map<String, Object> currentState = stateConfig.get(currentState2.name());
        if (currentState == null) return false;
        
        @SuppressWarnings("unchecked")
        List<String> nextStates = (List<String>) currentState.get("nextStates");
        return nextStates.contains(next.name());
    }
    
    public String getStateDescription(GameState.State state) {
        Map<String, Object> stateInfo = stateConfig.get(state.name());
        return stateInfo != null ? (String) stateInfo.get("description") : "";
    }
    
    public void validateStateTransition(State currentState, State nextState) {
        if (!isValidTransition(currentState, nextState)) {
            String message = String.format(
                "Invalid state transition from %s to %s", 
                currentState.name(), 
                nextState.name()
            );
            throw new IllegalStateException(message);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<State> getNextPossibleStates(State currentState) {
        Map<String, Object> state = stateConfig.get(currentState.name());
        if (state == null) {
            return Collections.emptyList();
        }
        
        List<String> nextStateNames = (List<String>) state.get("nextStates");
        return nextStateNames.stream()
            .map(State::valueOf)
            .collect(Collectors.toList());
    }
} 