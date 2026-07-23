package com.example.interviewfitbe.domain.followup.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FollowUpQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Interview 삭제 시 해당 면접의 꼬리질문도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 원 질문 삭제 시 해당 질문 기반 꼬리질문도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_question_id", nullable = false)
    private QuestionEntity sourceQuestion;

    /*
     * 원 답변 삭제 시 해당 답변 기반 꼬리질문도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_answer_id")
    private InterviewAnswerEntity sourceAnswer;

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