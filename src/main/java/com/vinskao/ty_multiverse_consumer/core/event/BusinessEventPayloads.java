package com.vinskao.ty_multiverse_consumer.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 事件 payload 組裝工具
 *
 * <p>更新事件只保留 before/after 的「差異」而非整個資料物件，
 * 既符合稽核需求，也避免 stream 被完整實體塞爆。</p>
 *
 * @author TY Backend Team
 * @since 2026-08
 */
@Component
public class BusinessEventPayloads {

    /**
     * 不會寫進事件 payload 的欄位（機密或無稽核價值）
     */
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "password", "token", "jwt", "secret", "accessToken", "refreshToken");

    private final ObjectMapper objectMapper;

    public BusinessEventPayloads(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 把實體轉成扁平 Map 快照，並移除機密欄位
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> snapshot(Object entity) {
        if (entity == null) {
            return Map.of();
        }
        Map<String, Object> raw = objectMapper.convertValue(entity, Map.class);
        Map<String, Object> cleaned = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (!EXCLUDED_FIELDS.contains(key)) {
                cleaned.put(key, value);
            }
        });
        return cleaned;
    }

    /**
     * 組出 before/after 差異 payload
     *
     * <p>只列出實際變動的欄位；若 before 為空（找不到原資料）則只放 after 快照。</p>
     */
    public Map<String, Object> diff(Map<String, Object> before, Object afterEntity) {
        Map<String, Object> after = snapshot(afterEntity);
        if (before == null || before.isEmpty()) {
            return Map.of("after", after);
        }
        Map<String, Object> changedBefore = new LinkedHashMap<>();
        Map<String, Object> changedAfter = new LinkedHashMap<>();
        Set<String> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            Object oldValue = before.get(key);
            Object newValue = after.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                changedBefore.put(key, oldValue);
                changedAfter.put(key, newValue);
            }
        }
        return Map.of(
                "changedFields", changedBefore.keySet(),
                "before", changedBefore,
                "after", changedAfter);
    }

    /**
     * 承載 before 快照與 after 實體，讓更新流程能在同一條 reactive chain 內組出差異
     */
    public record Change<T>(Map<String, Object> before, T after) {
    }
}
