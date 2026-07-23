package com.example.interviewfitbe.domain.question.entity;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 어떤 면접에서 출제된 질문인지.
     * interview_id는 nullable 허용.
     * 면접이 삭제되어도 질문 데이터는 남길 수 있도록 SET_NULL.
     */
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 직무 기반 질문이면 연결.
     * 기본 질문이나 임시 질문은 jobRole이 없을 수 있음.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id")
    private JobRoleEntity jobRole;

    /*
     * 자기소개서 기반 질문이면 연결.
     * cover_letter_id는 nullable 허용.
     * 자기소개서가 삭제되어도 질문 자체는 남길 수 있도록 SET_NULL.
     */
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id")
    private CoverLetterEntity coverLetter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    /*
     * 면접에서 몇 번째 질문인지.
     * 예: 1, 2, 3
     */
    private Integer questionOrder;

    /*
     * 전체 면접 영상 기준 질문이 화면에 표시된 시각.
     * 단위: 초
     */
    private Double shownAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void assignInterview(InterviewEntity interview) {
        this.interview = interview;
    }

    public void assignOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public void assignShownAt(Double shownAt) {
        this.shownAt = shownAt;
    }
}