package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupQuestionRepository extends JpaRepository<GroupQuestionEntity, Long> {

    // 1~3라운드용: 직무 상관없이 공통 라운드 질문 조회
    List<GroupQuestionEntity> findAllByRoundNumberOrderByIdAsc(Integer roundNumber);

    List<GroupQuestionEntity> findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(Integer roundNumber);

    // 4라운드용: 특정 직무에 매핑된 직무 토의 질문 조회
    List<GroupQuestionEntity> findAllByRoundNumberAndJobRoleIdOrderByIdAsc(
            Integer roundNumber,
            Long jobRoleId
    );

    Optional<GroupQuestionEntity> findFirstByRoundNumberAndQuestionText(
            Integer roundNumber,
            String questionText
    );

    Optional<GroupQuestionEntity> findFirstByRoundNumberOrderByIdAsc(Integer roundNumber);

    Optional<GroupQuestionEntity> findFirstByRoundNumberAndJobRoleIdOrderByIdAsc(
            Integer roundNumber,
            Long jobRoleId
    );
}
