package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {

    // 특정 사용자의 모든 면접 세션을 조회 (면접 기록 리스트 확인용)
    List<InterviewEntity> findByUserId(Long userId);

    // 특정 사용자가 해당 직무로 본 면접 횟수 확인 (필요 시)
    long countByUserIdAndJobRoleId(Long userId, Long jobRoleId);
}