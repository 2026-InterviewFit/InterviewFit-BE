package com.example.interviewfitbe.domain.coverletter.service.embedding;

import com.example.interviewfitbe.domain.coverletter.dto.embedding.CoverLetterEmbeddingRequest;
import com.example.interviewfitbe.domain.coverletter.dto.embedding.CoverLetterEmbeddingResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CoverLetterEmbeddingClientService {
    private final WebClient fastApiWebClient;

    public CoverLetterEmbeddingResponse createCoverLetterEmbedding(List<String> contents) {
        CoverLetterEmbeddingRequest request = CoverLetterEmbeddingRequest.builder()
                .contents(contents)
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
