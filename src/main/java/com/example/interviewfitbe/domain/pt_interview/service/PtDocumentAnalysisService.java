package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RequiredArgsConstructor
@Service
public class PtDocumentAnalysisService {

    private final WebClient fastApiWebClient;

    public PtDocumentAnalyzeResponse analyzeDocument(
            String objectKey
    ) {

        validateObjectKey(objectKey);

        PtDocumentAnalyzeRequest request =
                new PtDocumentAnalyzeRequest(
                        objectKey
                );

        try {
            PtDocumentAnalyzeResponse response =
                    fastApiWebClient.post()
                            .uri("/apis/pt-document/analyze")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    PtDocumentAnalyzeResponse.class
                            )
                            .block();

            if (response == null) {
                log.error(
                        "FastAPI PT 문서 분석 응답이 없습니다. objectKey={}",
                        objectKey
                );

                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );
            }

            log.info(
                    "FastAPI PT 문서 분석 요청 성공: objectKey={}, totalSlides={}",
                    objectKey,
                    response.totalSlides()
            );

            return response;

        } catch (WebClientResponseException e) {

            log.error(
                    "FastAPI PT 문서 분석 HTTP 오류: objectKey={}, status={}, response={}",
                    objectKey,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );

        } catch (BaseException e) {
            throw e;

        } catch (Exception e) {

            log.error(
                    "FastAPI PT 문서 분석 호출 실패: objectKey={}, error={}",
                    objectKey,
                    e.getMessage(),
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }

    private void validateObjectKey(
            String objectKey
    ) {

        if (objectKey == null
                || objectKey.isBlank()) {

            throw new IllegalArgumentException(
                    "PT 발표자료 objectKey가 존재하지 않습니다."
            );
        }
    }
}