package com.example.interviewfitbe.domain.coverletter.entity;

import com.example.interviewfitbe.domain.coverletter.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
@Table(name = "cover_letter_processings")
public class CoverLetterProcessingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_id")
    private CoverLetterEntity coverLetter;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public void processing() {
        this.status = ProcessingStatus.PROCESSING;
    }

    public void completed() {
        this.status = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void failed() {
        this.status = ProcessingStatus.FAILED;
    }
}
