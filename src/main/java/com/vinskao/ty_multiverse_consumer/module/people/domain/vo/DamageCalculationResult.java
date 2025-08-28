package com.vinskao.ty_multiverse_consumer.module.people.domain.vo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 傷害計算結果實體類
 * 用於存儲異步傷害計算的結果
 */
@Entity
@Table(name = "damage_calculation_result")
@Data
@NoArgsConstructor
public class DamageCalculationResult {

    /**
     * 請求ID（主鍵）
     */
    @Id
    @Column(name = "request_id", length = 255)
    private String requestId;

    /**
     * 角色名稱
     */
    @Column(name = "character_name", length = 255)
    private String characterName;

    /**
     * 計算出的傷害值
     */
    @Column(name = "damage_value")
    private Integer damageValue;

    /**
     * 計算狀態：processing, success, error
     */
    @Column(name = "status", length = 50)
    private String status;

    /**
     * 錯誤信息（如果計算失敗）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 創建時間
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 完成時間
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 建構函數
     */
    public DamageCalculationResult(String requestId, String characterName) {
        this.requestId = requestId;
        this.characterName = characterName;
        this.status = "processing";
        this.createdAt = LocalDateTime.now();
    }
    
    // 手動添加 getter 方法
    public String getRequestId() {
        return requestId;
    }
    
    public String getCharacterName() {
        return characterName;
    }
    
    public Integer getDamageValue() {
        return damageValue;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    // 手動添加 setter 方法
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }
    
    public void setDamageValue(Integer damageValue) {
        this.damageValue = damageValue;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
