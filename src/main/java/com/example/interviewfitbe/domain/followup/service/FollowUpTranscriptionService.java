package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpTranscriptionResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpTranscriptionService {

    private final WebClient fastApiWebClient;


    /**
     * 꼬리질문 생성을 위한 답변 음성 STT.
     *
     * Spring
     * POST /api/followup/transcribe
     *
     * ↓
     *
     * FastAPI
     * POST /apis/speech/transcribe-file
     *
     * ↓
     *
     * {
     *   "text": "사용자가 실제로 말한 답변"
     * }
     */
    public FollowUpTranscriptionResponse transcribe(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "STT 변환을 위한 음성 파일이 필요합니다."
            );
        }


        try {

            /*
             * ByteArrayResource는 요청 재시도 시에도 같은 byte[]를 다시
             * 전송할 수 있다. FastAPI가 개발 중 재시작되거나 연결이 순간적으로
             * reset된 경우에 한해서 네트워크 요청을 1회 재시도한다.
             */
            ByteArrayResource resource =
                    new ByteArrayResource(
                            file.getBytes()
                    ) {

                        @Override
                        public String getFilename() {

                            String originalFilename =
                                    file.getOriginalFilename();

                            if (
                                    originalFilename == null
                                            || originalFilename.isBlank()
                            ) {

                                return "answer.webm";
                            }

                            return originalFilename;
                        }
                    };


            FollowUpTranscriptionResponse response =
                    fastApiWebClient
                            .post()
                            .uri(
                                    "/apis/speech/transcribe-file"
                            )
                            .contentType(
                                    MediaType.MULTIPART_FORM_DATA
                            )
                            .body(
                                    BodyInserters
                                            .fromMultipartData(
                                                    "file",
                                                    resource
                                            )
                            )
                            .retrieve()
                            .bodyToMono(
                                    FollowUpTranscriptionResponse.class
                            )

                            /*
                             * Connection reset / connection refused 같은
                             * 전송 계층 오류만 500ms 뒤 1회 재시도한다.
                             *
                             * 422(무음/너무 짧음) 같은 정상적인 API 오류는
                             * 재시도하지 않는다.
                             */
                            .retryWhen(
                                    Retry.backoff(
                                                    1,
                                                    Duration.ofMillis(500)
                                            )
                                            .filter(
                                                    throwable ->
                                                            throwable
                                                                    instanceof WebClientRequestException
                                            )
                                            .onRetryExhaustedThrow(
                                                    (retrySpec, signal) ->
                                                            signal.failure()
                                            )
                            )
                            .timeout(
                                    Duration.ofMinutes(3)
                            )
                            .block();


            if (
                    response == null
                            || response.getText() == null
                            || response
                            .getText()
                            .isBlank()
            ) {

                log.error(
                        "FastAPI STT 응답에 text가 없습니다."
                );

                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );
            }


            log.info(
                    "꼬리질문용 STT 완료 - textLength={}",
                    response
                            .getText()
                            .length()
            );


            return response;


        } catch (
                WebClientResponseException e
        ) {

            log.error(
                    "FastAPI STT 요청 실패 - status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            if (e.getStatusCode().value() == 422) {
                throw new BaseException(
                        FastApiErrorCode.STT_NO_SPEECH
                );
            }

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );


        } catch (
                WebClientRequestException e
        ) {

            log.error(
                    "FastAPI STT 연결 실패 - message={}",
                    e.getMessage(),
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );


        } catch (
                IOException e
        ) {

            log.error(
                    "STT 파일 읽기 실패",
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }
}
