package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterEmbeddingRequest;
import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterEmbeddingResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@RequiredArgsConstructor
@Service
public class CoverLetterEmbeddingClientService {
    private final WebClient fastApiWebClient;

    public CoverLetterEmbeddingResponse createCoverLetterEmbedding(String text) {
        CoverLetterEmbeddingRequest request = CoverLetterEmbeddingRequest.builder()
                .text(text)
                .build();

        try {
            return fastApiWebClient.post()
                    .uri("/apis/embedding/coverletter")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CoverLetterEmbeddingResponse.class)
                    .block();
        } catch (WebClientException e) {
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}
