package com.vinskao.ty_multiverse_consumer.module.people.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.vinskao.ty_multiverse_consumer.core.repository.StringPkRepository;
import com.vinskao.ty_multiverse_consumer.module.people.domain.vo.PeopleImage;

import java.util.List;

@Repository
public interface PeopleImageRepository extends StringPkRepository<PeopleImage> {
    
    PeopleImage findByCodeName(String codeName);
    
    boolean existsByCodeName(String codeName);
    
    // 新增：批量查詢，避免N+1問題
    @Query("SELECT p FROM PeopleImage p WHERE p.codeName IN :codeNames")
    List<PeopleImage> findByCodeNamesIn(@Param("codeNames") List<String> codeNames);
}
