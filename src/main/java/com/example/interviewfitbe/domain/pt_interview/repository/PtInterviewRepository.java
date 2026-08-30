package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PtInterviewRepository
        extends JpaRepository<PtInterviewEntity, Long> {

    Optional<PtInterviewEntity> findByInterviewId(
            Long interviewId
    );

    /*
     * PT 예상 질문 생성 시 사용하는 조회
     *
     * 동일한 PT 면접에 질문 생성 요청이 동시에 들어오는 경우
     * 중복 질문이 저장되는 것을 방지하기 위해
     * 해당 PT 면접 행에 비관적 쓰기 잠금을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM PtInterviewEntity p
            WHERE p.id = :ptInterviewId
            """)
    Optional<PtInterviewEntity> findByIdForUpdate(
            @Param("ptInterviewId") Long ptInterviewId
    );
}