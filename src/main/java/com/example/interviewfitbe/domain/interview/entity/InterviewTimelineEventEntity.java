package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.InterviewTimelineEventType;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
     * Interview 삭제 시 타임라인 이벤트도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /*
     * Video 삭제 시 해당 영상 기준 이벤트도 같이 삭제.
     * 영상 저장 전 이벤트는 null 가능.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private VideoEntity video;

    /*
     * Question 삭제 시 해당 질문 이벤트도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    /*
     * Answer 삭제 시 해당 답변 이벤트도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private InterviewAnswerEntity answer;

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