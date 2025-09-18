package com.vinskao.ty_multiverse_consumer.module.people.dao;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.vinskao.ty_multiverse_consumer.core.repository.StringPkRepository;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.People;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface PeopleRepository extends StringPkRepository<People> {

    Mono<People> findByName(String name);

    Mono<Boolean> existsByName(String name);

    // 新增：批量查詢，避免N+1問題
    @Query("SELECT * FROM people WHERE name IN (:names)")
    Flux<People> findByNamesIn(@Param("names") List<String> names);

    // 新增：只查詢名稱，避免載入所有欄位
    @Query("SELECT name FROM people")
    Flux<String> findAllNames();

    // 新增：根據單一屬性查詢
    @Query("SELECT * FROM people WHERE attributes IS NOT NULL AND attributes LIKE CONCAT('%', :attribute, '%')")
    Flux<People> findByAttributeContaining(@Param("attribute") String attribute);
}
