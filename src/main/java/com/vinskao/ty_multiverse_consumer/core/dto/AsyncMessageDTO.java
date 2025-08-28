package com.vinskao.ty_multiverse_consumer.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 異步消息傳輸對象
 * 統一的消息格式，用於接收來自 Producer 的請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncMessageDTO {
    
    /**
     * 請求ID（UUID）
     */
    private String requestId;
    
    /**
     * API端點路徑
     * 例如：/people/damageWithWeapon
     */
    private String endpoint;
    
    /**
     * HTTP方法
     * 例如：GET, POST, PUT, DELETE
     */
    private String method;
    
    /**
     * 請求負載數據
     * 可以是字符串、對象或數組
     */
    private Object payload;
    
    /**
     * 時間戳
     */
    private Long timestamp;
    
    // 手動添加 getter 方法以確保編譯成功
    public String getRequestId() {
        return requestId;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getMethod() {
        return method;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    // 手動添加 setter 方法
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public void setPayload(Object payload) {
        this.payload = payload;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
