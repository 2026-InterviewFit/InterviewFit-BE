package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingService;
import com.example.interviewfitbe.domain.interview.dto.InterviewAnalyzeRequest;
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

@RequiredArgsConstructor
@Service
public class InterviewService {
    private final EyeBlinkingService eyeBlinkingService;
    private final PoseService poseService;
    private final SpeechReportService speechReportService;

    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;
    private final InterviewRepository interviewRepository;

    @Transactional
    public void generateAnalysisResult(InterviewAnalyzeRequest request) {
        InterviewEntity interview = createInterview(request);

// 1. 눈 깜박임 분석 및 저장
        this.eyeBlinkingService.analyzeAndSave(request.getVideoObjectKey(), interview);

        // 2. 자세 분석 및 저장
        this.poseService.analyzeFullPosture(request.getVideoObjectKey(), interview);

        // 3. 말하기 분석 및 저장
//        this.speechReportService.saveReport();
    }

    private InterviewEntity createInterview(InterviewAnalyzeRequest request) {
//        UserEntity user = this.userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new IllegalArgumentException(""));

        // 유저 임시로 생성
        UserEntity user = this.userRepository.save(
                UserEntity.builder()
                        .email("aaa")
                        .name("aaa")
                        .build());
        JobRoleEntity jobRole = this.jobRoleRepository.findById(request.getJobRoleId())
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));

        InterviewEntity interview = InterviewEntity.builder()
                .user(user)
                .jobRole(jobRole)
                .build();

        return this.interviewRepository.save(interview);
    }
}
