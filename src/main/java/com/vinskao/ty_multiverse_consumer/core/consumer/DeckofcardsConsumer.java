package com.vinskao.ty_multiverse_consumer.core.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.config.RabbitMQConfig;
import com.vinskao.ty_multiverse_consumer.core.service.AsyncResultService;
import com.vinskao.ty_multiverse_consumer.module.blackjack.service.BJService;
import com.vinskao.ty_multiverse_consumer.core.dto.AsyncMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deckofcards MQ 消費者
 *
 * <p>處理 Backend 通過 MQ 發送的 Deckofcards 請求</p>
 * <p>支持 blackjack 遊戲的所有操作</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Component
public class DeckofcardsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DeckofcardsConsumer.class);

    @Autowired
    private BJService bjService;

    @Autowired
    private AsyncResultService asyncResultService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 處理 Deckofcards 請求
     */
    @RabbitListener(queues = RabbitMQConfig.DECKOFCARDS_QUEUE)
    public void handleDeckofcardsRequest(AsyncMessageDTO message) {
        logger.info("🎮 收到 Deckofcards 請求: requestId={}, endpoint={}, method={}",
                   message.getRequestId(), message.getEndpoint(), message.getMethod());

        try {
            String result = processDeckofcardsAction(message.getEndpoint(), message.getPayload());
            asyncResultService.sendCompletedResult(message.getRequestId(), result);

            logger.info("✅ Deckofcards 請求處理成功: requestId={}", message.getRequestId());
        } catch (Exception e) {
            logger.error("❌ Deckofcards 請求處理失敗: requestId={}, error={}",
                        message.getRequestId(), e.getMessage(), e);
            asyncResultService.sendFailedResult(message.getRequestId(), e.getMessage());
        }
    }

    /**
     * 處理不同的 blackjack 動作
     */
    private String processDeckofcardsAction(String endpoint, Object payload) throws Exception {
        logger.info("🎯 處理 Deckofcards 動作: endpoint={}", endpoint);

        // 提取動作名稱
        String action = extractActionFromEndpoint(endpoint);
        logger.info("🎯 提取的動作: action={}", action);

        Object result = null;

        switch (action) {
            case "start":
                logger.info("🎮 開始新遊戲");
                result = bjService.startNewGame();
                break;

            case "hit":
                logger.info("🎮 玩家要牌");
                result = bjService.playerHit();
                break;

            case "stand":
                logger.info("🎮 玩家停牌");
                result = bjService.playerStand();
                break;

            case "status":
                logger.info("🎮 獲取遊戲狀態");
                result = bjService.getGameStatus();
                break;

            case "double":
                logger.info("🎮 玩家加倍");
                result = bjService.playerDouble();
                break;

            case "split":
                logger.info("🎮 玩家分牌");
                result = bjService.playerSplit();
                break;

            default:
                throw new IllegalArgumentException("未知的 Deckofcards 動作: " + action);
        }

        // 將結果轉換為 JSON
        String jsonResult = objectMapper.writeValueAsString(result);
        logger.info("🎮 動作處理完成: action={}, result={}", action, jsonResult);

        return jsonResult;
    }

    /**
     * 從 endpoint 提取動作名稱
     *
     * 例如: "/blackjack/start" -> "start"
     */
    private String extractActionFromEndpoint(String endpoint) {
        if (endpoint == null || !endpoint.contains("/")) {
            throw new IllegalArgumentException("無效的 endpoint: " + endpoint);
        }

        // 移除開頭的斜槓
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }

        // 提取最後一部分
        String[] parts = endpoint.split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("endpoint 格式錯誤: " + endpoint);
        }

        return parts[parts.length - 1]; // 最後一部分就是動作名稱
    }
}
