package com.example.interviewfitbe.domain.eye_blinking.entity;

import com.example.interviewfitbe.domain.eye_blinking.enums.EyeBlinkingEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_blinking_bad_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeBlinkingBadLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * EyeBlinkingReport 삭제 시 bad log도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eye_blinking_report_id", nullable = false)
    private EyeBlinkingReportEntity eyeBlinkingReportEntity;

    /*
     * 전체 면접 영상 기준 이벤트 발생 시각.
     * 단위: 초
     */
    @Column(nullable = false)
    private Double timestamp;

    /*
     * 이상 이벤트 지속 시간.
     */
    private Double duration;

    /*
     * LONG_CLOSED, RAPID_BLINKING, NO_BLINK_LONG.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EyeBlinkingEventType eventType;

    /*
     * 해당 구간의 국소 깜빡임률.
     */
    private Double blinkRateLocal;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}