package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PtQuestionRepository
        extends JpaRepository<PtQuestionEntity, Long> {

    List<PtQuestionEntity>
    findAllByPtInterviewIdOrderByQuestionNumberAsc(
            Long ptInterviewId
    );

    void deleteAllByPtInterviewId(
            Long ptInterviewId
    );

    /*
     * PT 질문 단건 조회
     */
    @EntityGraph(
            attributePaths = {
                    "ptInterview",
                    "ptInterview.interview"
            }
    )
    Optional<PtQuestionEntity>
    findWithPtInterviewById(
            Long id
    );

    /*
     * 특정 PT 면접의 질문 개수 조회
     */
    long countByPtInterview_Id(
            Long ptInterviewId
    );

    /*
     * 특정 PT 면접의 마지막 질문 조회
     */
    Optional<PtQuestionEntity>
    findTopByPtInterview_IdOrderByQuestionNumberDesc(
            Long ptInterviewId
    );
}
