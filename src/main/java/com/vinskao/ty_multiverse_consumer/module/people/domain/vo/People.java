package com.vinskao.ty_multiverse_consumer.module.people.domain.vo;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人物實體類別，代表系統中的人物資訊
 * 
 * <p>此類別包含人物的基本資訊、屬性、背景等詳細資料，
 * 支援樂觀鎖定機制防止並發更新衝突。</p>
 * 
 * @author TY Backend Team
 * @version 1.0
 * @since 2024
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table("people")
@JsonPropertyOrder({
    "nameOriginal", "codeName", "name", "physicPower", "magicPower", "utilityPower",
    "dob", "race", "attributes", "gender", "assSize", "boobsSize", "heightCm", "weightKg",
    "profession", "combat", "favoriteFoods", "job", "physics", "knownAs", "personality",
    "interest", "likes", "dislikes", "concubine", "faction", "armyId", "armyName",
    "deptId", "deptName", "originArmyId", "originArmyName", "gaveBirth", "email", "age",
    "proxy", "baseAttributes", "bonusAttributes", "stateAttributes"
})
public class People {
    
    /**
     * 人物原始名稱
     * <p>儲存人物的原始或真實姓名</p>
     */
    @JsonProperty("nameOriginal")
    @Column("name_original")
    private String nameOriginal;

    /**
     * 人物代號
     * <p>用於識別人物的代號或暱稱</p>
     */
    @JsonProperty("codeName")
    @Column("code_name")
    private String codeName;

    /**
     * 人物名稱（主鍵）
     * <p>作為實體的唯一識別符</p>
     */
    @JsonProperty("name")
    @Id
    private String name;

    /**
     * 物理力量
     * <p>表示人物的物理攻擊和防禦能力</p>
     */
    @JsonProperty("physicPower")
    @Column("physic_power")
    private Integer physicPower;

    /**
     * 魔法力量
     * <p>表示人物的魔法攻擊和防禦能力</p>
     */
    @JsonProperty("magicPower")
    @Column("magic_power")
    private Integer magicPower;

    /**
     * 實用能力
     * <p>表示人物的輔助和實用技能能力</p>
     */
    @JsonProperty("utilityPower")
    @Column("utility_power")
    private Integer utilityPower;

    /**
     * 出生日期
     * <p>人物的出生日期，格式為字串</p>
     */
    @JsonProperty("dob")
    @Column("dob")
    private String dob;

    /**
     * 種族
     * <p>人物的種族或物種</p>
     */
    @JsonProperty("race")
    @Column("race")
    private String race;

    /**
     * 屬性
     * <p>人物的基本屬性描述</p>
     */
    @JsonProperty("attributes")
    @Column("attributes")
    private String attributes;

    /**
     * 性別
     * <p>人物的性別</p>
     */
    @JsonProperty("gender")
    @Column("gender")
    private String gender;

    /**
     * 臀部尺寸
     * <p>人物的臀部尺寸描述</p>
     */
    @JsonProperty("assSize")
    @Column("ass_size")
    private String assSize;

    /**
     * 胸部尺寸
     * <p>人物的胸部尺寸描述</p>
     */
    @JsonProperty("boobsSize")
    @Column("boobs_size")
    private String boobsSize;

    /**
     * 身高（公分）
     * <p>人物的身高，單位為公分</p>
     */
    @JsonProperty("heightCm")
    @Column("height_cm")
    private Integer heightCm;

    /**
     * 體重（公斤）
     * <p>人物的體重，單位為公斤</p>
     */
    @JsonProperty("weightKg")
    @Column("weight_kg")
    private Integer weightKg;

    /**
     * 職業
     * <p>人物的主要職業或工作</p>
     */
    @JsonProperty("profession")
    @Column("profession")
    private String profession;

    /**
     * 戰鬥能力
     * <p>人物的戰鬥技能和經驗描述</p>
     */
    @JsonProperty("combat")
    @Column("combat")
    private String combat;

    /**
     * 喜愛的食物
     * <p>人物喜歡的食物清單</p>
     */
    @JsonProperty("favoriteFoods")
    @Column("favorite_foods")
    private String favoriteFoods;

    /**
     * 工作
     * <p>人物目前的工作或職務</p>
     */
    @JsonProperty("job")
    @Column("job")
    private String job;

    /**
     * 物理特性
     * <p>人物的物理特徵描述</p>
     */
    @JsonProperty("physics")
    @Column("physics")
    private String physics;

    /**
     * 別名
     * <p>人物被稱為的其他名稱</p>
     */
    @JsonProperty("knownAs")
    @Column("known_as")
    private String knownAs;

    /**
     * 個性
     * <p>人物的性格特徵描述</p>
     */
    @JsonProperty("personality")
    @Column("personality")
    private String personality;

    /**
     * 興趣
     * <p>人物的興趣愛好</p>
     */
    @JsonProperty("interest")
    @Column("interest")
    private String interest;

    /**
     * 喜好
     * <p>人物喜歡的事物</p>
     */
    @JsonProperty("likes")
    @Column("likes")
    private String likes;

    /**
     * 厭惡
     * <p>人物不喜歡的事物</p>
     */
    @JsonProperty("dislikes")
    @Column("dislikes")
    private String dislikes;

    /**
     * 後宮
     * <p>人物的後宮相關資訊</p>
     */
    @JsonProperty("concubine")
    @Column("concubine")
    private String concubine;

    /**
     * 派系
     * <p>人物所屬的派系或組織</p>
     */
    @JsonProperty("faction")
    @Column("faction")
    private String faction;

    /**
     * 軍隊編號
     * <p>人物所屬軍隊的編號</p>
     */
    @JsonProperty("armyId")
    @Column("army_id")
    private Integer armyId;

    /**
     * 軍隊名稱
     * <p>人物所屬軍隊的名稱</p>
     */
    @JsonProperty("armyName")
    @Column("army_name")
    private String armyName;

    /**
     * 部門編號
     * <p>人物所屬部門的編號</p>
     */
    @JsonProperty("deptId")
    @Column("dept_id")
    private Integer deptId;

    /**
     * 部門名稱
     * <p>人物所屬部門的名稱</p>
     */
    @JsonProperty("deptName")
    @Column("dept_name")
    private String deptName;

    /**
     * 原始軍隊編號
     * <p>人物最初所屬軍隊的編號</p>
     */
    @JsonProperty("originArmyId")
    @Column("origin_army_id")
    private Integer originArmyId;

    /**
     * 原始軍隊名稱
     * <p>人物最初所屬軍隊的名稱</p>
     */
    @JsonProperty("originArmyName")
    @Column("origin_army_name")
    private String originArmyName;

    /**
     * 是否生育
     * <p>標示人物是否已經生育</p>
     */
    @JsonProperty("gaveBirth")
    @Column("gave_birth")
    private Boolean gaveBirth;

    /**
     * 電子郵件
     * <p>人物的電子郵件地址</p>
     */
    @JsonProperty("email")
    @Column("email")
    private String email;

    /**
     * 年齡
     * <p>人物的年齡</p>
     */
    @JsonProperty("age")
    @Column("age")
    private Integer age;

    /**
     * 代理
     * <p>人物的代理或代表資訊</p>
     */
    @JsonProperty("proxy")
    @Column("proxy")
    private String proxy;

    /**
     * 基礎屬性
     * <p>人物的基礎屬性資料，通常為JSON格式</p>
     */
    @JsonProperty("baseAttributes")
    @Column("base_attributes")
    private String baseAttributes;

    /**
     * 加成屬性
     * <p>人物的加成屬性資料，通常為JSON格式</p>
     */
    @JsonProperty("bonusAttributes")
    @Column("bonus_attributes")
    private String bonusAttributes;

    /**
     * 狀態屬性
     * <p>人物的狀態屬性資料，通常為JSON格式</p>
     */
    @JsonProperty("stateAttributes")
    @Column("state_attributes")
    private String stateAttributes;

    /**
     * 嵌入向量
     * <p>用於語義搜尋的嵌入向量，通常由外部AI嵌入服務產生
     * 此欄位不映射到資料庫，僅用於記憶體中的語義搜尋功能</p>
     */
    @JsonProperty("embedding")
    @Transient
    private String embedding;

    /**
     * 建立時間
     * <p>記錄建立此人物資料的時間戳記</p>
     */
    @JsonProperty("createdAt")
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新時間
     * <p>記錄最後更新此人物資料的時間戳記</p>
     */
    @JsonProperty("updatedAt")
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 樂觀鎖定版本字段
     * <p>用於防止並發更新衝突，每次更新時版本號會自動遞增</p>
     */
    @JsonProperty("version")
    @Version
    private Long version;

    /**
     * 比較此人物與指定物件是否相等
     * 
     * @param o 要比較的物件
     * @return 如果相等則返回true，否則返回false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        People people = (People) o;
        return Objects.equals(name, people.name) &&
                Objects.equals(baseAttributes, people.baseAttributes) &&
                Objects.equals(bonusAttributes, people.bonusAttributes) &&
                Objects.equals(stateAttributes, people.stateAttributes) &&
                Objects.equals(nameOriginal, people.nameOriginal) &&
                Objects.equals(embedding, people.embedding) &&
                Objects.equals(createdAt, people.createdAt) &&
                Objects.equals(updatedAt, people.updatedAt);
    }

    /**
     * 計算此人物物件的雜湊碼
     * 
     * @return 雜湊碼值
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, baseAttributes, bonusAttributes, stateAttributes, nameOriginal, 
                          embedding, createdAt, updatedAt);
    }

    /**
     * 返回此人物物件的字串表示
     * 
     * @return 包含主要屬性的字串表示
     */
    @Override
    public String toString() {
        return "People{" +
                "name='" + name + '\'' +
                ", baseAttributes='" + baseAttributes + '\'' +
                ", bonusAttributes='" + bonusAttributes + '\'' +
                ", stateAttributes='" + stateAttributes + '\'' +
                ", nameOriginal='" + nameOriginal + '\'' +
                ", embedding='" + embedding + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
    
    // 手動添加缺失的 getter 方法
    public Integer getPhysicPower() {
        return physicPower;
    }
    
    public Integer getMagicPower() {
        return magicPower;
    }
    
    public Integer getUtilityPower() {
        return utilityPower;
    }
    
    public String getAttributes() {
        return attributes; // 返回正確的 attributes 字段
    }
    
    public String getStateAttributes() {
        return stateAttributes;
    }
}
