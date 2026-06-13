package com.example.interviewfitbe.domain.followup.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
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
     * 꼬리질문이 어떤 면접 세션에서 생성된 것인지 연결한다.
     *
     * 한 면접에서 꼬리질문이 여러 개 생성될 수도 있으므로
     * OneToOne보다 ManyToOne이 안전하다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 실제 Gemini/FastAPI에 전달한 직무명.
     * interview.jobRole.name에서 가져오거나, jobRoleId/job fallback으로 가져온다.
     */
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