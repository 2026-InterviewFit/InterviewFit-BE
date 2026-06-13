package com.example.interviewfitbe.domain.eye_blinking.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
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

    /*
     * 새로 추가된 InterviewEntity와 연결한다.
     *
     * 기존에는 userId, interviewId를 Long 값으로만 저장했지만,
     * 이제는 interview_id FK로 면접 세션과 연결하는 구조가 더 적절하다.
     *
     * nullable을 강제하지 않은 이유:
     * - Swagger 단독 테스트나 임시 테스트에서는 interviewId 없이도 분석할 수 있게 하기 위함
     * - 실제 서비스에서는 request에 interviewId를 넣어서 저장하면 됨
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * Spring이 저장한 영상 파일 경로.
     * 로컬 실행 시: /Users/.../uploads/eye-blinking/xxx.webm
     * Docker 실행 시: /shared/uploads/eye-blinking/xxx.webm 같은 공유 경로 권장
     */
    @Column(columnDefinition = "TEXT")
    private String videoUrl;

    private Integer blinkCount;

    private Double blinkRate;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    /*
     * FastAPI에서 받은 원본 분석 결과 JSON 저장.
     * 나중에 min_ear, max_ear, blink_timestamps 같은 디버깅 데이터도 확인 가능.
     */
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    private LocalDateTime createdAt;
}