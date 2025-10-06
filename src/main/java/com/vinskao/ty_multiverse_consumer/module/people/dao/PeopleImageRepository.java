package com.vinskao.ty_multiverse_consumer.module.people.dao;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.vinskao.ty_multiverse_consumer.core.repository.StringPkRepository;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.PeopleImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@Repository
public interface PeopleImageRepository extends StringPkRepository<PeopleImage> {

    Mono<PeopleImage> findByCodeName(String codeName);

    Mono<Boolean> existsByCodeName(String codeName);

    // 新增：批量查詢，避免N+1問題
    @Query("SELECT * FROM people_image WHERE codeName IN (:codeNames)")
    Flux<PeopleImage> findByCodeNamesIn(@Param("codeNames") List<String> codeNames);
}
