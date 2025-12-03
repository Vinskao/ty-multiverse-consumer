package com.vinskao.ty_multiverse_consumer.module.people.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.junit.jupiter.api.Assertions.*;

@JsonTest
public class PeopleDeserializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testDeserializeAllFields() throws Exception {
        String json = """
            {
                "nameOriginal": "Wavo Yuropha",
                "codeName": "Wavo Yuropha",
                "name": "Wavo",
                "physicPower": 5,
                "magicPower": 13111,
                "utilityPower": 4,
                "dob": "1994-11-12",
                "race": "Interstellar Prophetic Singularity Human",
                "attributes": "淫",
                "gender": "M",
                "assSize": "na",
                "boobsSize": "na",
                "heightCm": 177,
                "weightKg": 60,
                "profession": "King",
                "combat": "lewd",
                "favoriteFoods": "Hot Soup",
                "job": "Chef",
                "physics": "test physics",
                "knownAs": "Boss",
                "personality": "test personality",
                "interest": "Public humiliation",
                "likes": "test likes",
                "dislikes": "test dislikes",
                "concubine": "test",
                "faction": "Lily Palais",
                "armyId": 1,
                "armyName": "王",
                "deptId": 1,
                "deptName": "王室",
                "originArmyId": 1,
                "originArmyName": "王",
                "gaveBirth": false,
                "email": "test@example.com",
                "age": 31,
                "proxy": "test proxy"
            }
            """;

        People people = objectMapper.readValue(json, People.class);

        // 驗證所有字段都被正確反序列化
        assertNotNull(people.getNameOriginal(), "nameOriginal should not be null");
        assertNotNull(people.getCodeName(), "codeName should not be null");
        assertNotNull(people.getName(), "name should not be null");
        assertNotNull(people.getDob(), "dob should not be null");
        assertNotNull(people.getRace(), "race should not be null");
        assertNotNull(people.getGender(), "gender should not be null");
        assertNotNull(people.getAssSize(), "assSize should not be null");
        assertNotNull(people.getBoobsSize(), "boobsSize should not be null");
        assertNotNull(people.getProfession(), "profession should not be null");
        assertNotNull(people.getCombat(), "combat should not be null");
        assertNotNull(people.getFavoriteFoods(), "favoriteFoods should not be null");
        assertNotNull(people.getJob(), "job should not be null");
        assertNotNull(people.getPhysics(), "physics should not be null");
        assertNotNull(people.getKnownAs(), "knownAs should not be null");
        assertNotNull(people.getPersonality(), "personality should not be null");
        assertNotNull(people.getInterest(), "interest should not be null");
        assertNotNull(people.getLikes(), "likes should not be null");
        assertNotNull(people.getDislikes(), "dislikes should not be null");
        assertNotNull(people.getConcubine(), "concubine should not be null");
        assertNotNull(people.getEmail(), "email should not be null");
        assertNotNull(people.getProxy(), "proxy should not be null");

        // 打印所有字段的值
        System.out.println("nameOriginal: " + people.getNameOriginal());
        System.out.println("codeName: " + people.getCodeName());
        System.out.println("dob: " + people.getDob());
        System.out.println("race: " + people.getRace());
        System.out.println("gender: " + people.getGender());
        System.out.println("profession: " + people.getProfession());
        System.out.println("job: " + people.getJob());
        System.out.println("physics: " + people.getPhysics());
        System.out.println("email: " + people.getEmail());
    }
}
