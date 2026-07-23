package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Service
public class PtDocumentAnalysisService {

    private final WebClient fastApiWebClient;

    public PtDocumentAnalyzeResponse analyzeDocument(
            String objectKey
    ) {

        PtDocumentAnalyzeRequest request =
                new PtDocumentAnalyzeRequest(
                        objectKey
                );

        try {

            return fastApiWebClient.post()
                    .uri("/apis/pt-document/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(
                            PtDocumentAnalyzeResponse.class
                    )
                    .toFuture()
                    .join();

        } catch (WebClientResponseException e) {

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }
}
