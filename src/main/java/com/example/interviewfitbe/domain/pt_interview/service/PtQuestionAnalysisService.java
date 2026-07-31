package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionGenerateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionGenerateResponse;
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
public class PtQuestionAnalysisService {

    private final WebClient fastApiWebClient;

    /*
     * FastAPI PT 예상 질문 생성 호출
     *
     * POST /apis/pt-question/generate
     */
    public PtQuestionGenerateResponse generateQuestions(
            String documentSummary,
            String slidesText,
            Integer questionCount
    ) {

        validateRequest(
                documentSummary,
                slidesText,
                questionCount
        );

        PtQuestionGenerateRequest request =
                new PtQuestionGenerateRequest(
                        documentSummary,
                        slidesText,
                        questionCount
                );

        try {
            PtQuestionGenerateResponse response =
                    fastApiWebClient
                            .post()
                            .uri("/apis/pt-question/generate")
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(
                                    PtQuestionGenerateResponse.class
                            )
                            .block();

            if (response == null) {
                log.error(
                        "FastAPI PT 질문 생성 응답이 없습니다. questionCount={}",
                        questionCount
                );

                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );
            }

            log.info(
                    "FastAPI PT 질문 생성 성공: requestedCount={}, receivedCount={}",
                    questionCount,
                    response.questions() == null
                            ? 0
                            : response.questions().size()
            );

            return response;

        } catch (WebClientResponseException e) {

            log.error(
                    "FastAPI PT 질문 생성 HTTP 오류: status={}, response={}",
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
                    "FastAPI PT 질문 생성 호출 실패: error={}",
                    e.getMessage(),
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }

    /*
     * FastAPI 요청값 검증
     */
    private void validateRequest(
            String documentSummary,
            String slidesText,
            Integer questionCount
    ) {

        if (documentSummary == null
                || documentSummary.isBlank()) {

            throw new IllegalArgumentException(
                    "PT 문서 요약이 존재하지 않습니다."
            );
        }

        if (slidesText == null
                || slidesText.isBlank()) {

            throw new IllegalArgumentException(
                    "PT 슬라이드 내용이 존재하지 않습니다."
            );
        }

        if (questionCount == null
                || questionCount <= 0) {

            throw new IllegalArgumentException(
                    "PT 질문 개수는 1개 이상이어야 합니다."
            );
        }
    }
}