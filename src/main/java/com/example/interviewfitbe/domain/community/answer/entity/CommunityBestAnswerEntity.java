package com.example.interviewfitbe.domain.community.answer.entity;

import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "community_best_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_community_best_answer_source_answer",
                        columnNames = "source_answer_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityBestAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공개한 사용자.
     * 응답 시 닉네임은 마스킹해서 내려준다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 공개 시점의 지원 직무.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRoleEntity jobRole;

    /**
     * 원본 interview_answers.id.
     * 커뮤니티에는 답변 스냅샷을 저장하므로 FK로 묶지 않는다.
     * 사용자가 기존 분석 기록을 삭제해도 이미 공개한 커뮤니티 글은 유지된다.
     */
    @Column(name = "source_answer_id", nullable = false, unique = true)
    private Long sourceAnswerId;

    /**
     * 원본 interviews.id 추적용.
     */
    @Column(name = "source_interview_id", nullable = false)
    private Long sourceInterviewId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    /**
     * 공개 시점의 종합 점수 스냅샷 (0~100).
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * 공개 시점의 STT 답변 스크립트 스냅샷.
     */
    @Column(name = "answer_script", nullable = false, columnDefinition = "TEXT")
    private String answerScript;

    /**
     * 좋아요 기능은 다음 단계에서 연결한다.
     * 지금은 Feed 정렬/표시를 위해 컬럼만 준비한다.
     */
    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
