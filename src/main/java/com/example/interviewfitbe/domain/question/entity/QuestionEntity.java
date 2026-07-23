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

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    @ManyToOne(fetch = FetchType.LAZY)
    private JobRoleEntity jobRole;


    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id")
    private CoverLetterEntity coverLetter;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void assignInterview(InterviewEntity interview) {
        this.interview = interview;
    }
}