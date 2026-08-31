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
@Table(name = "cover_letter_question_embeddings")
public class CoverLetterQuestionEmbeddingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_question_id", nullable = false)
    private CoverLetterQuestionEntity coverLetterQuestion;

    @Column(nullable = false, columnDefinition = "vector(384)")
    private String embedding;
}
