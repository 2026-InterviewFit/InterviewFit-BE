package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.InterviewTimelineEventType;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_timeline_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewTimelineEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 어떤 면접 세션에서 발생한 이벤트인지.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /*
     * 전체 면접 영상.
     * 영상 저장 전에도 이벤트가 찍힐 수 있으므로 nullable 허용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private VideoEntity video;

    /*
     * 어떤 질문과 관련된 이벤트인지.
     * 면접 시작/종료 이벤트는 question이 없을 수 있음.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    /*
     * 어떤 답변 구간과 관련된 이벤트인지.
     * 답변 row 생성 전 이벤트는 null일 수 있음.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private InterviewAnswerEntity answer;

    /*
     * 이벤트 종류.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewTimelineEventType eventType;

    /*
     * 전체 면접 영상 기준 이벤트 발생 시각.
     * 단위: 초
     */
    @Column(nullable = false)
    private Double timestamp;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}