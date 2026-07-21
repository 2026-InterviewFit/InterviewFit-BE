package com.example.interviewfitbe.domain.coverletter.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "cover_letter_question_id", nullable = false)
    private CoverLetterQuestionEntity coverLetterQuestion;

    @Column(nullable = false, columnDefinition = "vector(384)")
    private String embedding;
}
