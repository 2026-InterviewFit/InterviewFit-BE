package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisRequest;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PoseAnalysisService {
    private final WebClient fastApiWebClient;

    public List<PoseAnalysisResponse> analyzePosture(String videoObjectKey) {
        PoseAnalysisRequest request = PoseAnalysisRequest.builder()
                .videoObjectKey(videoObjectKey)
                .build();

        try {
            return this.fastApiWebClient.post()
                    .uri("/pose/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(PoseAnalysisResponse.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("FRAME_NOT_FOUND")) {
                throw new BaseException(FastApiErrorCode.FRAME_NOT_FOUND);
            }

            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}
