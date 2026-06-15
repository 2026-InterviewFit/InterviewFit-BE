package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.dto.InterviewAnalyzeRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.exception.JobRoleErrorCode;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.pose.service.PoseService;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InterviewService {

    private final EyeBlinkingService eyeBlinkingService;
    private final PoseService poseService;
    private final SpeechReportService speechReportService;

    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;
    private final InterviewRepository interviewRepository;

    /**
     * 면접 시작 API에서 호출하는 메서드.
     *
     * 역할:
     * 1. userId가 있으면 기존 유저 조회
     * 2. userId가 없으면 임시 유저 생성
     * 3. jobRoleId로 직무 조회
     * 4. interview row 생성
     * 5. FE에 interviewId 반환
     */
    @Transactional
    public InterviewStartResponse startInterview(InterviewStartRequest request) {
        if (request.getJobRoleId() == null) {
            throw new IllegalArgumentException("jobRoleId는 필수입니다.");
        }

        UserEntity user = findOrCreateUser(request.getUserId());

        JobRoleEntity jobRole = jobRoleRepository.findById(request.getJobRoleId())
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));

        InterviewEntity interview = InterviewEntity.builder()
                .user(user)
                .jobRole(jobRole)
                .build();

        InterviewEntity savedInterview = interviewRepository.save(interview);

        return InterviewStartResponse.builder()
                .interviewId(savedInterview.getId())
                .userId(user.getId())
                .jobRoleId(jobRole.getId())
                .jobName(jobRole.getName())
                .status("STARTED")
                .build();
    }

    /**
     * 기존 통합 분석 API.
     *
     * 지금은 유지한다.
     * 나중에는 질문별 answerId 기반 비동기 분석으로 바꿀 예정.
     */
    @Transactional
    public void generateAnalysisResult(InterviewAnalyzeRequest request) {
        InterviewEntity interview = createInterview(request);

        // 1. 눈 깜빡임 분석 및 저장
        this.eyeBlinkingService.analyzeAndSave(request.getVideoObjectKey(), interview);

        // 2. 자세 분석 및 저장
        this.poseService.analyzeFullPosture(request.getVideoObjectKey(), interview);

        // 3. 말하기 분석 및 저장
        // this.speechReportService.saveReport();
    }

    private InterviewEntity createInterview(InterviewAnalyzeRequest request) {
        UserEntity user = findOrCreateUser(request.getUserId());

        JobRoleEntity jobRole = this.jobRoleRepository.findById(request.getJobRoleId())
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));

        InterviewEntity interview = InterviewEntity.builder()
                .user(user)
                .jobRole(jobRole)
                .build();

        return this.interviewRepository.save(interview);
    }

    /**
     * 로그인 기능이 완전히 붙기 전까지 안전하게 쓰는 유저 처리 메서드.
     *
     * userId가 있으면 기존 유저 사용.
     * userId가 없으면 임시 유저 생성.
     */
    private UserEntity findOrCreateUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 유저를 찾을 수 없습니다. userId=" + userId
                    ));
        }

        String tempEmail = "temp_" + UUID.randomUUID() + "@interviewfit.local";

        UserEntity tempUser = UserEntity.builder()
                .email(tempEmail)
                .name("임시 사용자")
                .build();

        return userRepository.save(tempUser);
    }
}