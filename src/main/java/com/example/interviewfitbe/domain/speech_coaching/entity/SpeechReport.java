// package com.example.interviewfitbe.domain.speech_coaching.entity;
package com.example.interviewfitbe.domain.speech_coaching.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speech_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpeechReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String textScript;            // 추출된 대본

    private String jobCategory;           // 직무 카테고리

    // 간투어 분석
    private Integer totalFillerCount;

    @Column(columnDefinition = "TEXT")
    private String fillerDetailsJson;     // {"음":3,"어":2,...} 를 JSON 문자열로 저장

    // 발화 속도
    private String paceEvaluation;        // 적절함/너무 느림/너무 빠름
    @Column(columnDefinition = "TEXT")
    private String paceFeedback;
    private Double charactersPerSecond;

    // 침묵 분석
    private Double totalSilenceSeconds;
    private Integer criticalSilenceCount;

    // Gemini AI 평가 결과 (STAR + keywords)
    @Column(columnDefinition = "TEXT")
    private String aiEvaluationJson;

    @CreationTimestamp
    private LocalDateTime createdAt;
}