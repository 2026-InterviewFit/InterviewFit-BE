package com.example.interviewfitbe.domain.eye_blinking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_blinking_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeBlinkingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long interviewId;

    @Column(columnDefinition = "TEXT")
    private String videoUrl;

    private Integer blinkCount;

    private Double blinkRate;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    private LocalDateTime createdAt;
}