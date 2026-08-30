package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "interview_questions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_questions_interview_question",
                        columnNames = {"interview_id", "question_id"}
                ),
                @UniqueConstraint(
                        name = "uk_interview_questions_interview_order",
                        columnNames = {"interview_id", "question_order"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_interview_questions_interview",
                        columnList = "interview_id"
                ),
                @Index(
                        name = "idx_interview_questions_question",
                        columnList = "question_id"
                )
        }
)
public class InterviewQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이번 질문 세트를 사용하는 실제 면접 세션.
     *
     * Interview가 삭제되면 그 면접의 연결 정보도 함께 삭제한다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /**
     * 질문 원본.
     *
     * QuestionEntity는 여러 Interview에서 재사용될 수 있으므로
     * QuestionEntity 자체에는 interview_id를 기록하지 않는다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    /**
     * 해당 면접 안에서의 질문 순서. 1부터 시작한다.
     */
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void changeOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
}
