package com.example.interviewfitbe.domain.coverletter.service.client;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterClientRequest;
import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterClientResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Service
public class CoverLetterClientService {
    private final WebClient fastApiWebClient;

    public CoverLetterClientResponse analyzeCoverLetter(String coverletter) {
        CoverLetterClientRequest request = CoverLetterClientRequest.builder()
                .coverletter(coverletter)
                .build();

        try {
            return fastApiWebClient.post()
                    .uri("/apis/coverletter/analysis")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CoverLetterClientResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}
