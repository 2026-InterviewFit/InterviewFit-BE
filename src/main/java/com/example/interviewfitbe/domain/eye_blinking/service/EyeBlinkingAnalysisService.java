package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class EyeBlinkingAnalysisService {

    private final WebClient fastApiWebClient;

    public Map<String, Object> analyzeBlinking(String videoObjectKey) {
        EyeBlinkingAnalyzeRequest request = new EyeBlinkingAnalyzeRequest();
        request.setVideoUrl(videoObjectKey);

        try {
            Map<String, Object> response = this.fastApiWebClient.post()
                    .uri("/apis/eye-blinking/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
            }

            return response;

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("NoSuchKey") || body.contains("FRAME_NOT_FOUND")) {
                throw new BaseException(FastApiErrorCode.FRAME_NOT_FOUND);
            }

            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}