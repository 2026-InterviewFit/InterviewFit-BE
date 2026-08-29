package com.example.interviewfitbe.domain.community.wiki.service;

import com.example.interviewfitbe.domain.community.wiki.dto.CommunityWikiCreateRequest;
import com.example.interviewfitbe.domain.community.wiki.dto.CommunityWikiQuestionResponse;
import com.example.interviewfitbe.domain.community.wiki.entity.CommunityWikiQuestionEntity;
import com.example.interviewfitbe.domain.community.wiki.entity.CommunityWikiVerificationEntity;
import com.example.interviewfitbe.domain.community.wiki.enums.CommunityWikiStatus;
import com.example.interviewfitbe.domain.community.wiki.repository.CommunityWikiQuestionRepository;
import com.example.interviewfitbe.domain.community.wiki.repository.CommunityWikiVerificationRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityWikiService {

    private static final Set<String> ALLOWED_TIERS = Set.of(
            "대기업",
            "중견기업",
            "중소기업",
            "스타트업"
    );

    private final CommunityWikiQuestionRepository questionRepository;
    private final CommunityWikiVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;

    @Transactional(readOnly = true)
    public List<CommunityWikiQuestionResponse> getQuestions(
            Long currentUserId
    ) {
        return questionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(question -> CommunityWikiQuestionResponse.from(
                        question,
                        currentUserId != null
                                && verificationRepository
                                .existsByWikiQuestion_IdAndUser_Id(
                                        question.getId(),
                                        currentUserId
                                )
                ))
                .toList();
    }

    @Transactional
    public CommunityWikiQuestionResponse createQuestion(
            Long userId,
            CommunityWikiCreateRequest request
    ) {
        validateCreateRequest(request);

        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다. userId=" + userId
                        )
                );

        JobRoleEntity jobRole = jobRoleRepository
                .findByName(request.getRole().trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 직무입니다: " + request.getRole()
                        )
                );

        CommunityWikiQuestionEntity question =
                CommunityWikiQuestionEntity.builder()
                        .user(user)
                        .jobRole(jobRole)
                        .companyName(request.getCompanyName().trim())
                        .companyTier(request.getTier().trim())
                        .questionText(request.getQuestionText().trim())
                        .verifiedCount(0)
                        .status(CommunityWikiStatus.PENDING)
                        .build();

        CommunityWikiQuestionEntity saved =
                questionRepository.save(question);

        return CommunityWikiQuestionResponse.from(
                saved,
                false
        );
    }

    @Transactional
    public CommunityWikiQuestionResponse verifyQuestion(
            Long userId,
            Long wikiQuestionId
    ) {
        CommunityWikiQuestionEntity question = questionRepository
                .findById(wikiQuestionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "기출문제를 찾을 수 없습니다. id=" + wikiQuestionId
                        )
                );

        UserEntity user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다. userId=" + userId
                        )
                );

        boolean alreadyVerified = verificationRepository
                .existsByWikiQuestion_IdAndUser_Id(
                        wikiQuestionId,
                        userId
                );

        if (alreadyVerified) {
            throw new IllegalStateException(
                    "이미 검증에 참여한 기출문제입니다."
            );
        }

        verificationRepository.save(
                CommunityWikiVerificationEntity.builder()
                        .wikiQuestion(question)
                        .user(user)
                        .build()
        );

        question.addVerification();

        return CommunityWikiQuestionResponse.from(
                question,
                true
        );
    }

    private void validateCreateRequest(
            CommunityWikiCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "기출문제 등록 정보가 필요합니다."
            );
        }

        if (isBlank(request.getCompanyName())) {
            throw new IllegalArgumentException(
                    "기업명을 입력해주세요."
            );
        }

        if (request.getCompanyName().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "기업명은 100자 이하로 입력해주세요."
            );
        }

        if (isBlank(request.getTier())
                || !ALLOWED_TIERS.contains(request.getTier().trim())) {
            throw new IllegalArgumentException(
                    "기업 규모는 대기업, 중견기업, 중소기업, 스타트업 중 하나여야 합니다."
            );
        }

        if (isBlank(request.getRole())) {
            throw new IllegalArgumentException(
                    "지원 직무를 선택해주세요."
            );
        }

        if (isBlank(request.getQuestionText())) {
            throw new IllegalArgumentException(
                    "기출 질문 내용을 입력해주세요."
            );
        }

        if (request.getQuestionText().trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "기출 질문은 1000자 이하로 입력해주세요."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
