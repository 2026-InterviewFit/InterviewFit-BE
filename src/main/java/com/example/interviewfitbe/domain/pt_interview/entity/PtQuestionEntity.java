package com.example.interviewfitbe.domain.pt_interview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pt_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pt_question_interview_number",
                        columnNames = {
                                "pt_interview_id",
                                "question_number"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 하나의 PT 면접에는 여러 예상 질문이 존재한다.
     *
     * PtInterview 1 : N PtQuestion
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pt_interview_id",
            nullable = false
    )
    private PtInterviewEntity ptInterview;

    /*
     * 질문 순서
     *
     * 1, 2, 3...
     */
    @Column(nullable = false)
    private Integer questionNumber;

    /*
     * 실제 면접 질문
     */
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String question;

    /*
     * AI가 이 질문을 생성한 이유
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PtQuestionEntity create(
            PtInterviewEntity ptInterview,
            Integer questionNumber,
            String question,
            String reason
    ) {

        PtQuestionEntity entity =
                new PtQuestionEntity();

        entity.ptInterview = ptInterview;
        entity.questionNumber = questionNumber;
        entity.question = question;
        entity.reason = reason;
        entity.createdAt = LocalDateTime.now();

        return entity;
    }
}
