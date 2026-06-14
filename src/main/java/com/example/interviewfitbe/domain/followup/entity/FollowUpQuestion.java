package com.example.interviewfitbe.domain.followup.entity;

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

    /**
     * 어떤 면접 세션에서 생성된 꼬리질문인지 연결한다.
     * 한 면접에서 여러 꼬리질문이 생성될 수 있으므로 ManyToOne으로 둔다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /**
     * 어떤 원본 질문에서 파생된 꼬리질문인지 연결한다.
     * 팀원 question 도메인과 연결하면 나중에
     * '원본 질문 → 사용자 답변 → 꼬리질문' 흐름을 추적하기 쉽다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    private String job;

    @Column(columnDefinition = "TEXT")
    private String originalQuestion;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String followUpQuestion;

    private String interviewStyle;

    @CreationTimestamp
    private LocalDateTime createdAt;
}