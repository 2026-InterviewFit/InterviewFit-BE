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

    // 어떤 면접에서 생성된 꼬리질문인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    // 어떤 원 질문에서 파생된 꼬리질문인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_question_id", nullable = false)
    private QuestionEntity sourceQuestion;

    // 어떤 원 답변을 보고 생성된 꼬리질문인지
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