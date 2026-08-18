package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "group_interview_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_answer_turn",
                columnNames = {
                        "group_interview_id",
                        "participant_user_id",
                        "round_number",
                        "question_index"
                }
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterviewAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_interview_id", nullable = false)
    private GroupInterviewEntity groupInterview;

    @Column(name = "participant_user_id", nullable = false)
    private Long participantUserId;

    @Column(name = "group_question_id")
    private Long questionId;

    @Column(name = "round_number", nullable = false)
    private Integer round;

    @Column(name = "question_index", nullable = false)
    private Integer questionIndex;

    @Column(name = "answer_started_at", nullable = false)
    private Instant answerStartedAt;

    @Column(name = "answer_completed_at")
    private Instant answerCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type")
    private GroupAnswerCompletionType completionType;

    @Column(name = "video_object_key", unique = true)
    private String videoObjectKey;

    @Column(name = "video_original_file_name")
    private String videoOriginalFileName;

    @Column(name = "video_content_type")
    private String videoContentType;

    @Column(name = "video_file_size")
    private Long videoFileSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public void complete(GroupAnswerCompletionType type, Instant completedAt) {
        if (this.completionType != null) {
            return;
        }
        this.completionType = type;
        this.answerCompletedAt = completedAt;
    }

    public void attachVideo(
            String objectKey,
            String originalFileName,
            String contentType,
            long fileSize
    ) {
        if (this.videoObjectKey != null) {
            throw new IllegalStateException("이미 영상이 업로드된 답변입니다.");
        }
        this.videoObjectKey = objectKey;
        this.videoOriginalFileName = originalFileName;
        this.videoContentType = contentType;
        this.videoFileSize = fileSize;
    }

    public boolean isVideoUploaded() {
        return videoObjectKey != null;
    }
}
