package com.example.interviewfitbe.domain.followup.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FollowUpQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 어떤 면접에서 생성된 꼬리질문인지.
     * 단독 테스트 또는 임시 생성 가능성을 고려해 nullable 허용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 원본 질문.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_question_id")
    private QuestionEntity sourceQuestion;

    /*
     * 어떤 답변을 보고 생성된 꼬리질문인지.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_answer_id")
    private InterviewAnswerEntity sourceAnswer;

    /*
     * 꼬리질문을 questions 테이블에 따로 저장하지 않기로 했으면
     * 이 필드는 없어도 됨.
     *
     * 다만 기존 표에 generated_question_id가 있었으므로
     * 확장 가능성 때문에 nullable로 남김.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_question_id")
    private QuestionEntity generatedQuestion;

    private String job;

    @Column(columnDefinition = "TEXT")
    private String originalQuestion;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String followUpQuestion;

    private String interviewStyle;

    @Column(columnDefinition = "TEXT")
    private String retrievedContextJson;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}