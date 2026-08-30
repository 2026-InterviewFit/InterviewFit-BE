package com.example.interviewfitbe.domain.community.wiki.entity;

import com.example.interviewfitbe.domain.community.wiki.enums.CommunityWikiStatus;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_wiki_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityWikiQuestionEntity {

    public static final int REQUIRED_VERIFICATION_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_role_id", nullable = false)
    private JobRoleEntity jobRole;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "company_tier", nullable = false, length = 20)
    private String companyTier;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Builder.Default
    @Column(name = "verified_count", nullable = false)
    private int verifiedCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private CommunityWikiStatus status = CommunityWikiStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addVerification() {
        this.verifiedCount += 1;

        if (this.verifiedCount >= REQUIRED_VERIFICATION_COUNT) {
            this.status = CommunityWikiStatus.VERIFIED;
        }
    }
}
