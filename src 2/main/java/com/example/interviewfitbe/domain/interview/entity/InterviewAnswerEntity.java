package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 전체 면접 세션
     *
     * 예:
     * interview 1개 안에
     * Q1 답변 영상, Q2 답변 영상, Q3 답변 영상이 여러 개 들어간다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /**
     * 질문 ID.
     * 아직 질문 엔티티와 직접 관계를 맺지 않고 Long으로 둔다.
     * 나중에 QuestionEntity FK로 바꿔도 됨.
     */
    @Column(name = "question_id")
    private Long questionId;

    /**
     * 질문 텍스트 스냅샷.
     * 질문 내용이 나중에 바뀌어도 당시 면접 질문을 보존하기 위함.
     */
    @Column(columnDefinition = "TEXT")
    private String questionText;

    /**
     * MinIO objectKey.
     *
     * 예:
     * videos/uuid_interview.webm
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String videoObjectKey;

    /**
     * 몇 번째 답변인지.
     *
     * 예:
     * Q1 = 1
     * Q2 = 2
     * Q3 = 3
     */
    @Column(nullable = false)
    private Integer answerOrder;

    /**
     * 이 답변 영상의 분석 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus analysisStatus;

    /**
     * 분석 실패 시 에러 메시지 저장.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void markUploaded() {
        this.analysisStatus = AnalysisStatus.UPLOADED;
        this.errorMessage = null;
    }

    public void markRunning() {
        this.analysisStatus = AnalysisStatus.RUNNING;
        this.errorMessage = null;
    }

    public void markDone() {
        this.analysisStatus = AnalysisStatus.DONE;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}