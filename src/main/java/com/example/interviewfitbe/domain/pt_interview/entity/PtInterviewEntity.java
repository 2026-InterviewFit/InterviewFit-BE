package com.example.interviewfitbe.domain.pt_interview.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.enums.PtInterviewStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pt_interview")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtInterviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 기존 InterviewEntity와 1:1 연결
     *
     * import 경로는 현재 프로젝트의
     * InterviewEntity 위치에 맞게 추가해야 함.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "interview_id",
            nullable = false,
            unique = true
    )
    private InterviewEntity interview;

    /*
     * PT 발표 주제
     */
    @Column(nullable = false)
    private String topic;

    /*
     * 발표 안내문 / 지시사항
     */
    @Column(columnDefinition = "TEXT")
    private String instruction;

    /*
     * 발표 준비 시간(초)
     */
    private Integer preparationSeconds;

    /*
     * 발표 제한 시간(초)
     */
    private Integer presentationSeconds;

    /*
     * PT 면접 진행 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PtInterviewStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /*
     * 생성 메서드
     */
    public static PtInterviewEntity create(
            InterviewEntity interview,
            String topic,
            String instruction,
            Integer preparationSeconds,
            Integer presentationSeconds
    ) {

        PtInterviewEntity entity = new PtInterviewEntity();

        entity.interview = interview;
        entity.topic = topic;
        entity.instruction = instruction;
        entity.preparationSeconds = preparationSeconds;
        entity.presentationSeconds = presentationSeconds;
        entity.status = PtInterviewStatus.CREATED;
        entity.createdAt = LocalDateTime.now();

        return entity;
    }

    public void changeStatus(PtInterviewStatus status) {
        this.status = status;
    }
}
