package com.vinskao.ty_multiverse_consumer.module.blackjack.service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.module.blackjack.domain.Card;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

public class Deck {
    protected List<Card> cards;
    // 卡牌配置
    protected Map<String, Map<String, String>> cardConfig;
    
    public Deck() {
        this.cards = new ArrayList<>();
        loadCardConfig();
        initDeck();
    }
    
    @SuppressWarnings("unchecked")
    private void loadCardConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 获取cards.json文件的输入流
            InputStream inputStream = getClass().getResourceAsStream("/cards.json");
            // 将输入流转换为Map
            cardConfig = mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load card configuration", e);
        }
    }

    public void initDeck() {
        // 清空cards
        cards.clear();
        // 遍历cardConfig的key，即花色
        for (String suit : cardConfig.keySet()) {
            Map<String, String> ranks = cardConfig.get(suit);
            // 遍历cardConfig的value，即花色对应的rank
            for (String rank : ranks.keySet()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    // 洗牌
    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card drawCard() {
        return cards.isEmpty() ? null : cards.remove(cards.size()-1);
    }

    public void reset() {
        initDeck();
    }

    public int remainingCards() {
        return cards.size();
    }
}
