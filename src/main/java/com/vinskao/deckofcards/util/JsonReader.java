package com.vinskao.deckofcards.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonReader {
    private final Map<String, Object> configCache;
    private final ObjectMapper mapper;
    
    public JsonReader() {
        this.configCache = new HashMap<>();
        this.mapper = new ObjectMapper();
    }
    
    // 读取配置文件
    @SuppressWarnings("unchecked")
    public <T> T readConfig(String configPath, Class<T> valueType) {
        try {
            if (configCache.containsKey(configPath)) {
                return (T) configCache.get(configPath);
            }
            ClassPathResource resource = new ClassPathResource(configPath);
            T config = mapper.readValue(resource.getInputStream(), valueType);
            // 将配置文件添加到缓存中
            configCache.put(configPath, config);
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + configPath, e);
        }
    }
    
    // 清除缓存
    public void clearCache() {
        configCache.clear();
    }
    
    // 从缓存中移除配置文件
    public void removeFromCache(String configPath) {
        configCache.remove(configPath);
    }
    
    // 添加專門用於讀取遊戲狀態配置的方法
    public Map<String, Map<String, Object>> readGameStateConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Object>>> config = 
            readConfig("states.json", Map.class);
            
        // 驗證配置
        validateGameStateConfig(config);
        
        return config.get("gameStates");
    }
    
    private void validateGameStateConfig(Map<String, Map<String, Map<String, Object>>> config) {
        if (config == null || !config.containsKey("gameStates")) {
            throw new IllegalStateException("Invalid game state configuration: missing gameStates");
        }
        
        Map<String, Map<String, Object>> states = config.get("gameStates");
        for (Map.Entry<String, Map<String, Object>> entry : states.entrySet()) {
            Map<String, Object> stateConfig = entry.getValue();
            if (!stateConfig.containsKey("description") || !stateConfig.containsKey("nextStates")) {
                throw new IllegalStateException(
                    "Invalid state configuration for state: " + entry.getKey()
                );
            }
        }
    }
}
