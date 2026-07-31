package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtReportGenerateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportGenerateResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PtReportAnalysisService {

    private final WebClient fastApiWebClient;

    public PtReportGenerateResponse generateReport(
            PtReportGenerateRequest request
    ) {
        validateRequest(request);

        try {
            PtReportGenerateResponse response =
                    fastApiWebClient
                            .post()
                            .uri("/apis/pt-report/generate")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(PtReportGenerateResponse.class)
                            .block();

            if (response == null) {
                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );
            }

            return response;

        } catch (BaseException e) {
            throw e;

        } catch (WebClientResponseException e) {
            log.error(
                    "PT 리포트 생성 실패. status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );

        } catch (Exception e) {
            log.error(
                    "PT 리포트 생성 실패",
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }

    private void validateRequest(
            PtReportGenerateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "PT 리포트 요청 정보가 존재하지 않습니다."
            );
        }
    }
}