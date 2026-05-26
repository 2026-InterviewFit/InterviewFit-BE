package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisRequest;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.question.dto.QuestionClientRequest;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionClientService {
    private final WebClient fastApiWebClient;

    public QuestionClientResponse createQuestions(Integer count, String jobType, String resume) {
        QuestionClientRequest request = QuestionClientRequest.builder()
                .count(count)
                .jobType(jobType)
                .resumeContent(resume)
                .build();

        return this.fastApiWebClient.post()
                .uri("/api/question/create")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QuestionClientResponse.class)
                .block();
    }
}
