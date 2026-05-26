package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpQuestionRepository followUpQuestionRepository;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {
        String fastApiUrl = "http://127.0.0.1:8000/api/followup/create";

        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> fastApiResult = restTemplate.postForObject(
                fastApiUrl,
                request,
                Map.class
        );

        String followUpQuestion = fastApiResult.get("follow_up_question").toString();

        FollowUpQuestion saved = followUpQuestionRepository.save(
                FollowUpQuestion.builder()
                        .userId(request.getUserId())
                        .interviewId(request.getInterviewId())
                        .job(request.getJob())
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
}