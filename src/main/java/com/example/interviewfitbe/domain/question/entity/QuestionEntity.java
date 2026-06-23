package com.example.interviewfitbe.domain.question.entity;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRoleEntity jobRole;

    // 자기소개서 (null 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id")
    private CoverLetterEntity coverLetter;

    public void linkInterviewAnswer(InterviewAnswerEntity interviewAnswer) {
        this.interviewAnswer = interviewAnswer;
    }
}
