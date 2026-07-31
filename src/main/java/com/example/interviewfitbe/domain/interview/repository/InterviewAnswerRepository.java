package com.example.interviewfitbe.domain.interview.repository;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewAnswerRepository
        extends JpaRepository<InterviewAnswerEntity, Long> {

    /*
     * 일반 면접의 답변 목록 조회
     */
    List<InterviewAnswerEntity>
    findByInterview_IdOrderByAnswerOrderAsc(
            Long interviewId
    );

    /*
     * 일반 면접 정보와 직무 정보를 포함하여
     * 답변 단건 조회
     */
    @EntityGraph(
            attributePaths = {
                    "interview",
                    "interview.jobRole"
            }
    )
    Optional<InterviewAnswerEntity>
    findWithInterviewById(
            Long id
    );

    /*
     * PT 면접의 전체 답변 목록 조회
     *
     * PtQuestion → PtInterview 관계를 이용하여 조회한다.
     */
    @EntityGraph(
            attributePaths = {
                    "ptQuestion",
                    "ptQuestion.ptInterview",
                    "ptQuestion.ptInterview.interview"
            }
    )
    List<InterviewAnswerEntity>
    findAllByPtQuestion_PtInterview_IdOrderByAnswerOrderAsc(
            Long ptInterviewId
    );

    /*
     * 특정 PT 질문의 가장 최근 답변 조회
     */
    @EntityGraph(
            attributePaths = {
                    "ptQuestion",
                    "ptQuestion.ptInterview"
            }
    )
    Optional<InterviewAnswerEntity>
    findTopByPtQuestion_IdOrderByIdDesc(
            Long ptQuestionId
    );

    /*
     * 특정 PT 질문에 저장된 답변이 있는지 확인
     */
    boolean existsByPtQuestion_Id(
            Long ptQuestionId
    );
}