package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final InterviewRepository interviewRepository;
    private final JobRoleService jobRoleService;
    private final WebClient webClient;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());

        /*
         * 직무명 결정 우선순위
         * 1. interviewId가 있으면 interview.jobRole.name 사용
         * 2. jobRoleId가 있으면 job_role 테이블에서 조회
         * 3. job 문자열이 있으면 그대로 사용
         */
        String jobName = resolveJobName(request, interview);

        Map<String, Object> fastApiRequest = new HashMap<>();
        fastApiRequest.put("job", jobName);
        fastApiRequest.put("original_question", request.getOriginalQuestion());
        fastApiRequest.put("answer", request.getAnswer());
        fastApiRequest.put("interview_style", request.getInterviewStyle());
        fastApiRequest.put("userId", request.getUserId());
        fastApiRequest.put("interviewId", request.getInterviewId());

        Map<String, Object> fastApiResult = webClient.post()
                .uri("/api/followup/create")
                .bodyValue(fastApiRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (fastApiResult == null || fastApiResult.get("follow_up_question") == null) {
            throw new RuntimeException("FastAPI 꼬리질문 생성 결과가 비어 있습니다.");
        }

        String followUpQuestion = fastApiResult.get("follow_up_question").toString();

        FollowUpQuestion saved = followUpQuestionRepository.save(
                FollowUpQuestion.builder()
                        .interview(interview)
                        .job(jobName)
                        .originalQuestion(request.getOriginalQuestion())
                        .answer(request.getAnswer())
                        .followUpQuestion(followUpQuestion)
                        .interviewStyle(request.getInterviewStyle())
                        .build()
        );

        return new FollowUpCreateResponse(
                "꼬리질문 생성 및 저장 완료",
                saved.getId(),
                followUpQuestion
        );
    }

    /*
     * interviewId가 있으면 InterviewEntity를 조회한다.
     * 없으면 단독 테스트용으로 null 허용.
     */
    private InterviewEntity findInterviewOrNull(Long interviewId) {
        if (interviewId == null) {
            return null;
        }

        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접 정보를 찾을 수 없습니다. interviewId=" + interviewId
                ));
    }

    private String resolveJobName(FollowUpCreateRequest request, InterviewEntity interview) {
        if (interview != null && interview.getJobRole() != null) {
            return interview.getJobRole().getName();
        }

        if (request.getJobRoleId() != null) {
            JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
            return jobRole.getName();
        }

        if (request.getJob() != null && !request.getJob().isBlank()) {
            return request.getJob();
        }

        throw new IllegalArgumentException("interviewId, jobRoleId 또는 job 값이 필요합니다.");
    }
}