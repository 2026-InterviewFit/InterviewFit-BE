package com.example.interviewfitbe.domain.coverletter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "cover_letter_questions")
public class CoverLetterQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id", nullable = false)
    private CoverLetterEntity coverLetter;

    // 자기소개서 문항
    @Column(columnDefinition = "TEXT")
    private String question;

    // 자기소개서 문항에 대한 답변
    @Column(columnDefinition = "TEXT")
    private String answer;

    // 자기소개서 문항 번호
    private Integer questionOrder;
}
