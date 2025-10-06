package com.vinskao.ty_multiverse_consumer.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Producer 回應傳輸對象
 * 統一的反應格式，用於發送回傳消息到 Producer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProducerResponseDTO {
    
    /**
     * 請求ID（UUID）
     */
    private String requestId;
    
    /**
     * 處理狀態
     * SUCCESS: 成功
     * ERROR: 錯誤
     * PROCESSING: 處理中
     */
    private String status;
    
    /**
     * 處理結果描述
     */
    private String message;
    
    /**
     * 處理結果數據
     * 可以是任何類型的數據
     */
    private Object data;
    
    /**
     * 錯誤代碼（僅在錯誤時使用）
     */
    private String errorCode;
    
    /**
     * 錯誤詳情（僅在錯誤時使用）
     */
    private String errorDetails;
    
    /**
     * 時間戳
     */
    private Long timestamp;
    
    /**
     * 創建成功回應的靜態方法
     */
    public static ProducerResponseDTO success(String requestId, String message, Object data) {
        return new ProducerResponseDTO(requestId, "SUCCESS", message, data, null, null, System.currentTimeMillis());
    }
    
    /**
     * 創建錯誤回應的靜態方法
     */
    public static ProducerResponseDTO error(String requestId, String message, String errorCode, String errorDetails) {
        return new ProducerResponseDTO(requestId, "ERROR", message, null, errorCode, errorDetails, System.currentTimeMillis());
    }
    
    /**
     * 創建處理中回應的靜態方法
     */
    public static ProducerResponseDTO processing(String requestId, String message) {
        return new ProducerResponseDTO(requestId, "PROCESSING", message, null, null, null, System.currentTimeMillis());
    }
}
