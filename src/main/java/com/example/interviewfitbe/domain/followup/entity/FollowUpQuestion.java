package com.example.interviewfitbe.domain.followup.entity;

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

    private Long userId;

    private Long interviewId;

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