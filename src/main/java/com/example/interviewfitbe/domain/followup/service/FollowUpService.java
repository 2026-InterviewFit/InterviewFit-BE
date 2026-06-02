package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
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
    private final JobRoleService jobRoleService;
    private final WebClient webClient;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {

        String jobName = resolveJobName(request);

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
                        .userId(request.getUserId())
                        .interviewId(request.getInterviewId())
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

    private String resolveJobName(FollowUpCreateRequest request) {
        if (request.getJobRoleId() != null) {
            JobRoleEntity jobRole = jobRoleService.getJobRoleEntity(request.getJobRoleId());
            return jobRole.getName();
        }

        if (request.getJob() != null && !request.getJob().isBlank()) {
            return request.getJob();
        }

        throw new IllegalArgumentException("jobRoleId 또는 job 값이 필요합니다.");
    }
}