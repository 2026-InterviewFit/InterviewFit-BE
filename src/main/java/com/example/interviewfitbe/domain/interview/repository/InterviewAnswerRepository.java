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
     * 답변 분석용 단건 조회
     *
     * 비동기 분석에서는 조회 트랜잭션이 끝난 뒤에도
     * interview / jobRole / 일반 질문 / PT 질문 정보가 필요하다.
     * 질문별 답변 분석에서는 InterviewQuestion -> Question 경로를 사용하므로
     * 해당 연관관계까지 EntityGraph로 미리 로딩한다.
     */
    @EntityGraph(
            attributePaths = {
                    "interview",
                    "interview.jobRole",
                    "interviewQuestion",
                    "interviewQuestion.question",
                    "question",
                    "ptQuestion",
                    "ptQuestion.ptInterview"
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
