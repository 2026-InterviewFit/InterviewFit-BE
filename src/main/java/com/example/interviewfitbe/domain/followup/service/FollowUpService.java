package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestion;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final WebClient webClient;

    @Transactional
    public FollowUpCreateResponse createAndSave(FollowUpCreateRequest request) {

        Map<String, Object> fastApiResult = webClient.post()
                .uri("/api/v1/interview/follow-up-question")
                .bodyValue(request)
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