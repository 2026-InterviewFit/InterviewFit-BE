package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.question.dto.QuestionClientRequest;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Service
public class QuestionClientService {
    private final WebClient fastApiWebClient;

    public QuestionClientResponse createQuestions(Integer count, String jobType, String coverLetter) {
        QuestionClientRequest request = QuestionClientRequest.builder()
                .count(count)
                .jobType(jobType)
                .coverLetter(coverLetter)
                .build();

        return this.fastApiWebClient.post()
                .uri("/api/question/create")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QuestionClientResponse.class)
                .block();
    }
}
